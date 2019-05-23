import {Component, OnDestroy, OnInit} from '@angular/core';
import {urlParamsStore} from "../../utils/navigation";
import {Cluster} from "../../../generated";
import {environment} from "../../../environments/environment";
import {DomSanitizer} from "@angular/platform-browser";
import {ClusterService as LeoClusterService} from "../../../notebooks-generated";
import {Headers, RequestMethod, RequestOptions, RequestOptionsArgs, Response} from "@angular/http";
import {Observable} from "rxjs";

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

  // This was added to cluster.service.ts
  // 
  // public readOnly(body: string, extraHttpRequestParams?: RequestOptionsArgs): Observable<Response> {
  //   let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845
  //
  //   // authentication (googleoauth) required
  //   if (this.configuration.accessToken) {
  //     const accessToken = typeof this.configuration.accessToken === 'function'
  //       ? this.configuration.accessToken()
  //       : this.configuration.accessToken;
  //     headers.set('Authorization', 'Bearer ' + accessToken);
  //   }
  //
  //   // to determine the Accept header
  //   let httpHeaderAccepts: string[] = [
  //     'application/json'
  //   ];
  //   const httpHeaderAcceptSelected: string | undefined = this.configuration.selectHeaderAccept(httpHeaderAccepts);
  //   if (httpHeaderAcceptSelected != undefined) {
  //     headers.set('Accept', httpHeaderAcceptSelected);
  //   }
  //
  //   // to determine the Content-Type header
  //   const consumes: string[] = [
  //     'application/json'
  //   ];
  //   const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
  //   if (httpContentTypeSelected != undefined) {
  //     headers.set('Content-Type', httpContentTypeSelected);
  //   }
  //
  //   let requestOptions: RequestOptionsArgs = new RequestOptions({
  //     method: RequestMethod.Post,
  //     headers: headers,
  //     body: body,
  //     withCredentials:this.configuration.withCredentials
  //   });
  //   // https://github.com/swagger-api/swagger-codegen/issues/4037
  //   if (extraHttpRequestParams) {
  //     requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
  //   }
  //
  //
  //   return this.http.request(`https://terra-calhoun-dev.appspot.com/api/convert`, requestOptions);
  // }
}
