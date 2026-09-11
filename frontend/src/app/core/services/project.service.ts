import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Project } from '../models/report.model';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/projects`;

  listActive(): Observable<Project[]> {
    const params = new HttpParams().set('activeOnly', 'true');
    return this.http.get<Project[]>(this.baseUrl, {
      params,
      withCredentials: true,
    });
  }

  listAll(): Observable<Project[]> {
    const params = new HttpParams().set('activeOnly', 'false');
    return this.http.get<Project[]>(this.baseUrl, {
      params,
      withCredentials: true,
    });
  }
}
