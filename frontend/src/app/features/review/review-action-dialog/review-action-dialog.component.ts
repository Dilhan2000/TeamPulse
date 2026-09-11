import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ReviewActionDialogData {
  action: 'APPROVE' | 'REQUEST_CHANGES';
  reportWeek: string;
  memberName: string;
}

export interface ReviewActionResult {
  confirmed: boolean;
  comment?: string;
}

@Component({
  selector: 'app-review-action-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './review-action-dialog.component.html',
  styleUrl: './review-action-dialog.component.scss',
})
export class ReviewActionDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ReviewActionDialogComponent, ReviewActionResult>);
  readonly data: ReviewActionDialogData = inject(MAT_DIALOG_DATA);

  comment: string = '';

  isCommentInvalid(): boolean {
    return !this.comment || this.comment.trim().length === 0;
  }

  onConfirm(): void {
    if (this.data.action === 'REQUEST_CHANGES' && this.isCommentInvalid()) {
      return;
    }
    this.dialogRef.close({
      confirmed: true,
      comment: this.comment.trim() ? this.comment.trim() : undefined,
    });
  }
}
