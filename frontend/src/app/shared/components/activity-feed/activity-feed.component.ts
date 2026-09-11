import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { ActivityFeedItem } from '../../../core/models/dashboard.model';

@Component({
  selector: 'app-activity-feed',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  templateUrl: './activity-feed.component.html',
  styleUrl: './activity-feed.component.scss',
})
export class ActivityFeedComponent {
  @Input() items: ActivityFeedItem[] = [];
  @Input() loading = false;
  @Output() itemSelected = new EventEmitter<ActivityFeedItem>();

  private readonly router = inject(Router);

  trackById(_index: number, item: ActivityFeedItem): string {
    return item.id || `${item.type}-${item.reportId}-${item.timestamp}`;
  }

  formatNarrative(item: ActivityFeedItem): string {
    if (item.type === 'SUBMISSION') {
      return `${item.actorName} submitted their Week of ${item.weekStartDate} report`;
    }
    if (item.action === 'APPROVED') {
      return `${item.actorName} approved ${item.targetUserName || 'member'}'s Week of ${item.weekStartDate} report`;
    }
    if (item.action === 'CHANGES_REQUESTED') {
      return `${item.actorName} requested changes on ${item.targetUserName || 'member'}'s Week of ${item.weekStartDate} report`;
    }
    return `${item.actorName} reviewed ${item.targetUserName || 'member'}'s Week of ${item.weekStartDate} report`;
  }

  getIconClass(item: ActivityFeedItem): string {
    if (item.type === 'SUBMISSION') return 'submission';
    if (item.action === 'APPROVED') return 'approved';
    return 'changes-requested';
  }

  getIconName(item: ActivityFeedItem): string {
    if (item.type === 'SUBMISSION') return 'send';
    if (item.action === 'APPROVED') return 'check_circle';
    return 'rate_review';
  }

  getActionChipClass(item: ActivityFeedItem): string {
    if (item.type === 'SUBMISSION') return 'submission';
    if (item.action === 'APPROVED') return 'approved';
    return 'changes-requested';
  }

  formatActionText(item: ActivityFeedItem): string {
    if (item.type === 'SUBMISSION') return 'Submitted';
    if (item.action === 'APPROVED') return 'Approved';
    if (item.action === 'CHANGES_REQUESTED') return 'Needs Correction';
    return item.action || '';
  }

  onItemClick(item: ActivityFeedItem): void {
    this.itemSelected.emit(item);
    if (item.reportId) {
      if (item.type === 'SUBMISSION') {
        this.router.navigate(['/manager/reports', item.reportId, 'review']);
      } else {
        this.router.navigate(['/reports', item.reportId]);
      }
    }
  }
}
