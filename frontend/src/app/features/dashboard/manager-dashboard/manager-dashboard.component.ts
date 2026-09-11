import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

import { DashboardMetricsService } from '../../../core/services/dashboard-metrics.service';
import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { AiChatService } from '../../../core/services/ai-chat.service';
import {
  ActivityFeedItem,
  DashboardSummary,
  StatusByMember,
  TimeByTaskType,
  TrendPoint,
  WorkloadByProject,
} from '../../../core/models/dashboard.model';
import { TeamMemberOption } from '../../../core/models/report.model';
import { ActivityFeedComponent } from '../../../shared/components/activity-feed/activity-feed.component';
import { SummaryDialogComponent } from './summary-dialog/summary-dialog.component';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatButtonToggleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
    BaseChartDirective,
    ActivityFeedComponent,
    DatePipe,
  ],
  templateUrl: './manager-dashboard.component.html',
  styleUrls: ['./manager-dashboard.component.scss'],
})
export class ManagerDashboardComponent implements OnInit {
  private readonly metricsService = inject(DashboardMetricsService);
  private readonly teamDashboardService = inject(TeamDashboardService);
  readonly aiChatService = inject(AiChatService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  // Loading states
  loadingSummary = signal(true);
  loadingTrend = signal(true);
  loadingStatus = signal(true);
  loadingWorkload = signal(true);
  loadingTimeType = signal(true);
  loadingFeed = signal(true);

  // Data signals
  summary = signal<DashboardSummary | null>(null);
  activityFeed = signal<ActivityFeedItem[]>([]);
  teamMembers = signal<TeamMemberOption[]>([]);
  selectedWeekStartDate = signal<string>('');

  // Filter selections
  selectedWeeks = 8;
  trendScope: 'ALL' | 'INDIVIDUAL' = 'ALL';
  selectedUserId?: number;

  // Chart 1: Tasks Completed Trend (Line)
  trendChartType: 'line' = 'line';
  trendChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Tasks Completed',
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.12)',
        fill: true,
        tension: 0.35,
        pointBackgroundColor: '#2563eb',
        pointBorderColor: '#fff',
        pointRadius: 4,
        pointHoverRadius: 6,
      },
    ],
  };
  trendChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => `Tasks Completed: ${ctx.parsed.y}`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        ticks: { stepSize: 1 },
        grid: { color: 'rgba(0, 0, 0, 0.05)' },
      },
    },
  };

  // Chart 2: Report Status by Team Member (Stacked Bar)
  statusChartType: 'bar' = 'bar';
  statusChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      { data: [], label: 'Approved', backgroundColor: '#10b981' },
      { data: [], label: 'Needs Correction', backgroundColor: '#f59e0b' },
      { data: [], label: 'Submitted', backgroundColor: '#3b82f6' },
      { data: [], label: 'Draft', backgroundColor: '#94a3b8' },
    ],
  };
  statusChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
    },
    scales: {
      x: {
        stacked: true,
        grid: { display: false },
      },
      y: {
        stacked: true,
        beginAtZero: true,
        ticks: { stepSize: 1 },
        grid: { color: 'rgba(0, 0, 0, 0.05)' },
      },
    },
  };

  // Chart 3: Workload by Project (Bar)
  workloadChartType: 'bar' = 'bar';
  workloadChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Hours Spent',
        backgroundColor: [
          '#3b82f6',
          '#8b5cf6',
          '#ec4899',
          '#f97316',
          '#10b981',
          '#06b6d4',
          '#6366f1',
        ],
        borderRadius: 4,
      },
    ],
  };
  workloadChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.parsed.y} hours`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        title: { display: true, text: 'Hours' },
        grid: { color: 'rgba(0, 0, 0, 0.05)' },
      },
    },
  };

  // Chart 4: Time by Task Type (Doughnut)
  taskTypeChartType: 'doughnut' = 'doughnut';
  taskTypeChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: ['Development', 'Testing', 'Meetings', 'Documentation', 'Other'],
    datasets: [
      {
        data: [0, 0, 0, 0, 0],
        backgroundColor: ['#6366f1', '#14b8a6', '#f59e0b', '#a855f7', '#64748b'],
        borderWidth: 2,
        borderColor: '#ffffff',
      },
    ],
  };
  taskTypeChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'right' },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.label}: ${ctx.parsed} hrs`,
        },
      },
    },
  };

  ngOnInit(): void {
    this.loadTeamMembers();
    this.loadAll();
  }

  loadTeamMembers(): void {
    this.teamDashboardService.listTeamMembers().subscribe({
      next: (members) => {
        this.teamMembers.set(members);
      },
      error: (err) => console.error('Failed to load team members', err),
    });
  }

  loadAll(): void {
    this.loadSummary();
    this.loadTrendChart();
    this.loadStatusChart();
    this.loadWorkloadChart();
    this.loadTimeTypeChart();
    this.loadActivityFeed();
  }

  loadSummary(): void {
    this.loadingSummary.set(true);
    this.metricsService.getSummary().subscribe({
      next: (res) => {
        this.summary.set(res);
        if (res.currentWeekStartDate && !this.selectedWeekStartDate()) {
          this.selectedWeekStartDate.set(res.currentWeekStartDate);
        }
        this.loadingSummary.set(false);
      },
      error: (err) => {
        console.error('Failed to load dashboard summary', err);
        this.loadingSummary.set(false);
      },
    });
  }

  openSummaryDialog(): void {
    const weekStartDate =
      this.selectedWeekStartDate() ||
      this.summary()?.currentWeekStartDate ||
      new Date().toISOString().split('T')[0];

    this.dialog.open(SummaryDialogComponent, {
      data: {
        weekStartDate,
      },
      width: '680px',
      maxWidth: '95vw',
    });
  }

  loadTrendChart(): void {
    this.loadingTrend.set(true);
    const userId = this.trendScope === 'INDIVIDUAL' ? this.selectedUserId : undefined;

    this.metricsService.getTasksCompletedTrend(this.selectedWeeks, userId).subscribe({
      next: (points: TrendPoint[]) => {
        this.trendChartData = {
          labels: points.map((p) => this.formatShortDate(p.weekStartDate)),
          datasets: [
            {
              ...this.trendChartData.datasets[0],
              data: points.map((p) => p.completedCount),
            },
          ],
        };
        this.loadingTrend.set(false);
      },
      error: (err) => {
        console.error('Failed to load trend chart', err);
        this.loadingTrend.set(false);
      },
    });
  }

  loadStatusChart(): void {
    this.loadingStatus.set(true);
    const { from, to } = this.calculateDateRange();
    this.metricsService.getStatusByMember(from, to).subscribe({
      next: (data: StatusByMember[]) => {
        this.statusChartData = {
          labels: data.map((d) => d.fullName),
          datasets: [
            { data: data.map((d) => d.approved), label: 'Approved', backgroundColor: '#10b981' },
            {
              data: data.map((d) => d.needsCorrection),
              label: 'Needs Correction',
              backgroundColor: '#f59e0b',
            },
            { data: data.map((d) => d.submitted), label: 'Submitted', backgroundColor: '#3b82f6' },
            { data: data.map((d) => d.draft), label: 'Draft', backgroundColor: '#94a3b8' },
          ],
        };
        this.loadingStatus.set(false);
      },
      error: (err) => {
        console.error('Failed to load status chart', err);
        this.loadingStatus.set(false);
      },
    });
  }

  loadWorkloadChart(): void {
    this.loadingWorkload.set(true);
    const { from, to } = this.calculateDateRange();

    this.metricsService.getWorkloadByProject(from, to).subscribe({
      next: (data: WorkloadByProject[]) => {
        this.workloadChartData = {
          labels: data.map((d) => d.projectName),
          datasets: [
            {
              ...this.workloadChartData.datasets[0],
              data: data.map((d) => d.totalHours),
            },
          ],
        };
        this.loadingWorkload.set(false);
      },
      error: (err) => {
        console.error('Failed to load workload chart', err);
        this.loadingWorkload.set(false);
      },
    });
  }

  loadTimeTypeChart(): void {
    this.loadingTimeType.set(true);
    const { from, to } = this.calculateDateRange();

    this.metricsService.getTimeByTaskType(from, to).subscribe({
      next: (data: TimeByTaskType[]) => {
        const typeOrder: Array<TimeByTaskType['taskType']> = [
          'DEVELOPMENT',
          'TESTING',
          'MEETINGS',
          'DOCUMENTATION',
          'OTHER',
        ];
        const hoursMap = new Map<string, number>();
        data.forEach((d) => hoursMap.set(d.taskType, d.totalHours));

        const orderedHours = typeOrder.map((t) => hoursMap.get(t) || 0);

        this.taskTypeChartData = {
          labels: ['Development', 'Testing', 'Meetings', 'Documentation', 'Other'],
          datasets: [
            {
              ...this.taskTypeChartData.datasets[0],
              data: orderedHours,
            },
          ],
        };
        this.loadingTimeType.set(false);
      },
      error: (err) => {
        console.error('Failed to load time by task type chart', err);
        this.loadingTimeType.set(false);
      },
    });
  }

  loadActivityFeed(): void {
    this.loadingFeed.set(true);
    this.metricsService.getActivityFeed(20).subscribe({
      next: (items) => {
        this.activityFeed.set(items);
        this.loadingFeed.set(false);
      },
      error: (err) => {
        console.error('Failed to load activity feed', err);
        this.loadingFeed.set(false);
      },
    });
  }

  onWeeksChange(): void {
    this.loadTrendChart();
    this.loadStatusChart();
    this.loadWorkloadChart();
    this.loadTimeTypeChart();
  }

  onTrendScopeChange(scope: 'ALL' | 'INDIVIDUAL'): void {
    this.trendScope = scope;
    if (scope === 'ALL') {
      this.selectedUserId = undefined;
    } else if (!this.selectedUserId && this.teamMembers().length > 0) {
      this.selectedUserId = this.teamMembers()[0].id;
    }
    this.loadTrendChart();
  }

  onMemberSelectChange(): void {
    this.loadTrendChart();
  }

  navigateToReviewQueue(): void {
    this.router.navigate(['/manager/team-reports']);
  }

  private calculateDateRange(): { from: string; to: string } {
    const today = new Date();
    const day = today.getDay();
    const diffToMonday = today.getDate() - day + (day === 0 ? -6 : 1);
    const thisMonday = new Date(today.getFullYear(), today.getMonth(), diffToMonday);

    // End on next Monday to include reports submitted in advance for the upcoming cycle
    const nextMonday = new Date(thisMonday);
    nextMonday.setDate(nextMonday.getDate() + 7);

    const fromDate = new Date(nextMonday);
    fromDate.setDate(fromDate.getDate() - (this.selectedWeeks - 1) * 7);

    return {
      from: this.formatDateIso(fromDate),
      to: this.formatDateIso(nextMonday),
    };
  }

  private formatDateIso(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatShortDate(isoString: string): string {
    if (!isoString) return '';
    const parts = isoString.split('-');
    if (parts.length === 3) {
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const m = parseInt(parts[1], 10) - 1;
      const d = parseInt(parts[2], 10);
      return `${monthNames[m]} ${d}`;
    }
    return isoString;
  }
}
