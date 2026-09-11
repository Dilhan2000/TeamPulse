import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-account-status',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './account-status.component.html',
  styleUrl: './account-status.component.scss',
})
export class AccountStatusComponent {
  readonly message: string;

  constructor() {
    const navState = history.state?.message;
    this.message = navState || 'Registration submitted. An administrator will review your account.';
  }
}
