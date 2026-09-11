import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AiChatService } from '../../../../core/services/ai-chat.service';

export interface SummaryDialogData {
  weekStartDate: string;
  projectId?: number;
}

@Component({
  selector: 'app-summary-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    DatePipe,
  ],
  templateUrl: './summary-dialog.component.html',
  styleUrls: ['./summary-dialog.component.scss'],
})
export class SummaryDialogComponent implements OnInit {
  private readonly aiChatService = inject(AiChatService);
  readonly dialogRef = inject(MatDialogRef<SummaryDialogComponent>);

  readonly loading = signal<boolean>(true);
  readonly summary = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly copied = signal<boolean>(false);

  constructor(@Inject(MAT_DIALOG_DATA) public data: SummaryDialogData) {}

  ngOnInit(): void {
    this.generateSummary();
  }

  generateSummary(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.aiChatService
      .generateSummary(this.data.weekStartDate, this.data.projectId)
      .subscribe({
        next: (res) => {
          this.summary.set(res.summary);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          if (err.status === 503) {
            this.errorMessage.set('AI assistant is not configured.');
          } else if (err.status === 502) {
            this.errorMessage.set(
              'AI assistant is temporarily unavailable — please try again.',
            );
          } else {
            this.errorMessage.set(
              err.error?.message || 'Failed to generate weekly team summary.',
            );
          }
        },
      });
  }

  copySummary(): void {
    const text = this.summary();
    if (!text) {
      return;
    }

    if (navigator?.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        this.copied.set(true);
        setTimeout(() => this.copied.set(false), 2500);
      });
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
