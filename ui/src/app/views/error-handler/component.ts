import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {StatusCheckService} from 'app/services/status-check.service';

@Component({
  selector: 'app-error-handler',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})
export class ErrorHandlerComponent implements OnInit {

  constructor(
    public errorHandlingService: ErrorHandlingService,
    public statusCheckService: StatusCheckService
  ) {}

  ngOnInit(): void {}

  closeServerError(): void {
    this.errorHandlingService.clearServerError();
  }

  closeNoServerResponse(): void {
    this.errorHandlingService.clearNoServerResponse();
  }

  closeServerBusy(): void {
    this.errorHandlingService.clearServerBusy();
  }

  acceptedUserDisabled(): void {
    this.errorHandlingService.clearUserDisabledError();
  }
}
