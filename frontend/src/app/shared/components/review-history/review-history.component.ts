import { Component, Input } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ReportReview } from '../../../core/models/report.model';

@Component({
  selector: 'app-review-history',
  standalone: true,
  imports: [CommonModule, MatIconModule, DatePipe],
  templateUrl: './review-history.component.html',
  styleUrl: './review-history.component.scss',
})
export class ReviewHistoryComponent {
  @Input() reviews: ReportReview[] = [];
}
