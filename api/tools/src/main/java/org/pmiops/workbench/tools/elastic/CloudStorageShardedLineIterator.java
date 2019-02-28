package org.pmiops.workbench.tools.elastic;

import com.google.cloud.storage.Blob;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * An iterator over a set of new-line delimited files in Google Cloud Storage. Intended to be
 * used in combination with BigQuery export, which can write newline delimited JSON or CSV files
 * when dealing with large result sets.
 */
public class CloudStorageShardedLineIterator implements Iterator<String> {

  private final Queue<Blob> blobs;
  private BufferedReader br;
  // Always buffer the next value so we can easily answer hasNext().
  private String next;

  CloudStorageShardedLineIterator(Iterable<Blob> blobs) {
    this.blobs = Lists.newLinkedList(blobs);
    this.next = readNext();
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public String next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    String ret = next;
    next = readNext();
    return ret;
  }

  private String readNext() {
    if (br == null) {
      br = openBlob(blobs.remove());
    }
    try {
      String line;
      while ((line = br.readLine()) == null) {
        if (blobs.isEmpty()) {
          return null;
        }
        br = openBlob(blobs.remove());
      }
      return line;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private BufferedReader openBlob(Blob b) {
    return new BufferedReader(new InputStreamReader(Channels.newInputStream(b.reader())));
  }
}
