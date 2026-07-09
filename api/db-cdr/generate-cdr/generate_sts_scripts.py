#!/usr/bin/env python3
"""
generate_sts_scripts.py

Reads a CSV of source/destination pairs and generates one shell script per row,
each containing a curl command to create a Google Cloud Storage Transfer Service job.

Usage:
    python3 generate_sts_scripts.py \
        --input transfers.csv \
        --project all-of-us-rw-prod \
        --output-dir ./scripts \
        [--service-account deploy@all-of-us-rw-prod.iam.gserviceaccount.com] \
        [--storage-class STANDARD]

CSV format (header row required):
    source,destination[,description]

    source      - Required. GCS URI. Trailing slash = folder transfer; no trailing slash = single file.
    destination - Required. GCS folder URI (will be treated as a folder).
    description - Optional. Human-readable label used in the job description and output filename.
"""

import argparse
import csv
import json
import os
import re
import sys
from datetime import date


def parse_gcs_uri(uri):
    """Split gs://bucket/path/to/object into (bucket, path)."""
    uri = uri.strip()
    if not uri.startswith("gs://"):
        raise ValueError(f"Not a valid GCS URI: {uri}")
    without_scheme = uri[len("gs://"):]
    parts = without_scheme.split("/", 1)
    bucket = parts[0]
    path = parts[1] if len(parts) > 1 else ""
    return bucket, path


def ensure_trailing_slash(path):
    return path if path.endswith("/") else path + "/"


def build_transfer_spec(source, destination, storage_class):
    """
    Build the transferSpec dict.

    - If source ends with '/', it's a folder transfer: gcsDataSource with a path.
    - Otherwise, it's a single-file transfer: gcsDataSource points at the parent
      folder, and objectConditions.includePrefixes filters to just the filename.
    """
    source_bucket, source_path = parse_gcs_uri(source)
    dest_bucket, dest_path = parse_gcs_uri(destination)
    dest_path = ensure_trailing_slash(dest_path)

    transfer_options = {"overwriteObjectsAlreadyExistingInSink": False}
    if storage_class and storage_class != "STANDARD":
        transfer_options["metadataOptions"] = {
            "storageClass": f"STORAGE_CLASS_{storage_class}"
        }

    is_folder = source.endswith("/")

    if is_folder:
        source_path = ensure_trailing_slash(source_path)
        gcs_data_source = {"bucketName": source_bucket, "path": source_path}
        transfer_spec = {
            "gcsDataSource": gcs_data_source,
            "gcsDataSink": {"bucketName": dest_bucket, "path": dest_path},
            "transferOptions": transfer_options,
        }
    else:
        # Single file: point source at the parent directory, filter by filename
        filename = os.path.basename(source_path)
        parent_path = os.path.dirname(source_path)
        parent_path = ensure_trailing_slash(parent_path) if parent_path else ""
        gcs_data_source = {"bucketName": source_bucket}
        if parent_path:
            gcs_data_source["path"] = parent_path
        transfer_spec = {
            "gcsDataSource": gcs_data_source,
            "gcsDataSink": {"bucketName": dest_bucket, "path": dest_path},
            "transferOptions": transfer_options,
            "objectConditions": {"includePrefixes": [filename]},
        }

    return transfer_spec


def build_job_config(row, index, today, project, service_account, storage_class):
    source = row["source"].strip()
    destination = row["destination"].strip()
    description = row.get("description", "").strip() or f"Transfer #{index}: {source} -> {destination}"

    transfer_spec = build_transfer_spec(source, destination, storage_class)

    job_config = {
        "projectId": project,
        "description": description,
        "status": "ENABLED",
        "schedule": {
            "scheduleStartDate": {"year": today.year, "month": today.month, "day": today.day},
            "scheduleEndDate":   {"year": today.year, "month": today.month, "day": today.day},
        },
        "transferSpec": transfer_spec,
    }

    if service_account:
        job_config["serviceAccount"] = service_account

    return job_config


def build_shell_script(row, index, today, project, service_account, storage_class):
    job_config = build_job_config(row, index, today, project, service_account, storage_class)
    source = row["source"].strip()
    destination = row["destination"].strip()

    lines = ["#!/bin/bash", ""]
    lines.append(f"# Transfer {index}: {row.get('description', '').strip() or source}")
    lines.append(f"# Source: {source}")
    lines.append(f"# Destination: {destination}")
    lines.append("")

    if service_account:
        lines.append(f"ACCESS_TOKEN=$(gcloud auth print-access-token --impersonate-service-account={service_account})")
    else:
        lines.append("ACCESS_TOKEN=$(gcloud auth print-access-token)")
    lines.append("")

    lines.append("curl -X POST \\")
    lines.append('    -H "Authorization: Bearer ${ACCESS_TOKEN}" \\')
    lines.append('    -H "Content-Type: application/json" \\')
    lines.append(f'    -H "X-Goog-User-Project: {project}" \\')
    lines.append('    "https://storagetransfer.googleapis.com/v1/transferJobs" \\')
    lines.append(f"    -d '{json.dumps(job_config, indent=2)}'")

    return "\n".join(lines) + "\n"


def sanitize_filename(text, max_len=40):
    """Turn a description or source path into a safe filename fragment."""
    text = re.sub(r"[^\w\-]", "_", text)
    text = re.sub(r"_+", "_", text).strip("_")
    return text[:max_len]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", required=True, help="Path to the input CSV file")
    parser.add_argument("--project", required=True, help="GCP billing/execution project, e.g. all-of-us-rw-prod")
    parser.add_argument("--output-dir", default=".", help="Directory to write the generated .sh scripts (default: current dir)")
    parser.add_argument("--service-account", default=None, help="Service account to impersonate (optional)")
    parser.add_argument("--storage-class", default="STANDARD", help="GCS storage class: STANDARD, NEARLINE, COLDLINE, ARCHIVE (default: STANDARD)")
    args = parser.parse_args()

    if not os.path.isfile(args.input):
        print(f"ERROR: input file not found: {args.input}", file=sys.stderr)
        sys.exit(1)

    os.makedirs(args.output_dir, exist_ok=True)
    today = date.today()

    required_columns = {"source", "destination"}

    with open(args.input, newline="") as f:
        reader = csv.DictReader(f)
        if not required_columns.issubset(set(reader.fieldnames or [])):
            missing = required_columns - set(reader.fieldnames or [])
            print(f"ERROR: CSV is missing required columns: {missing}", file=sys.stderr)
            sys.exit(1)

        rows = list(reader)

    print(f"Generating {len(rows)} script(s) in {args.output_dir}/")

    for i, row in enumerate(rows, start=1):
        try:
            script_content = build_shell_script(row, i, today, args.project, args.service_account, args.storage_class)
        except ValueError as e:
            print(f"ERROR on row {i}: {e}", file=sys.stderr)
            sys.exit(1)

        label = row.get("description", "").strip() or row["source"].strip()
        filename = f"transfer_{i:03d}_{sanitize_filename(label)}.sh"
        filepath = os.path.join(args.output_dir, filename)

        with open(filepath, "w") as out:
            out.write(script_content)
        os.chmod(filepath, 0o755)

        print(f"  {filename}")

    print("Done.")


if __name__ == "__main__":
    main()
