import {Component, OnDestroy, OnInit} from '@angular/core';
import {urlParamsStore} from "../../utils/navigation";
import {Cluster} from "../../../generated";
import {environment} from "../../../environments/environment";
import {DomSanitizer} from "@angular/platform-browser";
import {ClusterService as LeoClusterService} from "../../../notebooks-generated";

declare const gapi: any;

@Component({
  templateUrl: './component.html',
})
export class ReadOnlyNotebookComponent implements OnInit, OnDestroy {

  private readOnlyNotebook: string;

  constructor(private sanitizer: DomSanitizer,
              private leoClusterService: LeoClusterService) {}

  ngOnInit(): void {
    const {ns, wsid, nbName} = urlParamsStore.getValue();

    let url = this.notebookUrl(wsid, "all-of-us", nbName);
    let leoUrl = "https://leonardo.dsde-dev.broadinstitute.org/notebooks/aou-rw-local1-1933908639/all-of-us/api/contents/workspaces/duplicateoftestcreate/test.ipynb"

    gapi.client.storage.objects.get({
      bucket: 'fc-ef7e6293-0ba2-4116-8eac-a45884d82e25',
      object: 'notebooks/test.ipynb',
      alt: 'media'
    }).then(body => {
      this.leoClusterService.readOnly(body.body).subscribe(resp => {
        this.readOnlyNotebook = resp.text();
        let newWindow = window.open();
        newWindow.document.write(this.readOnlyNotebook);
      });
    });
  }

  ngOnDestroy(): void {}

  private notebookUrl(namespace: string, name: string, nbName: string): string {
    return encodeURI(
      environment.leoApiUrl + '/notebooks/'
      + namespace + '/'
      + name + '/notebooks/' + nbName);
  }
}
