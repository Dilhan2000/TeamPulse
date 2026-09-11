import { Component, Input } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-correction-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule, DatePipe],
  templateUrl: './correction-banner.component.html',
  styleUrl: './correction-banner.component.scss',
})
export class CorrectionBannerComponent {
  @Input() comment?: string | null;
  @Input() reviewerName?: string | null;
  @Input() reviewedAt?: string | null;
}
