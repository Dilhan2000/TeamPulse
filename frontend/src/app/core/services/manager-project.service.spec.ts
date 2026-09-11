import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ManagerProjectService } from './manager-project.service';
import { Project, ProjectMember } from '../models/project.model';
import { environment } from '../../../environments/environment';

describe('ManagerProjectService', () => {
  let service: ManagerProjectService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/manager/projects`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ManagerProjectService,
      ],
    });

    service = TestBed.inject(ManagerProjectService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should list projects with pagination and filters', () => {
    const mockPage = {
      content: [
        { id: 1, name: 'Project A', description: 'Desc A', active: true },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    };

    service.list({ search: 'alpha', active: true, page: 0, size: 10 }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].name).toBe('Project A');
    });

    const req = httpTesting.expectOne((r) =>
      r.url === baseUrl &&
      r.params.get('search') === 'alpha' &&
      r.params.get('active') === 'true' &&
      r.params.get('page') === '0' &&
      r.params.get('size') === '10'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should create project', () => {
    const newProject: Project = { id: 2, name: 'New Proj', description: 'Desc', active: true };

    service.create({ name: 'New Proj', description: 'Desc' }).subscribe((res) => {
      expect(res.id).toBe(2);
      expect(res.name).toBe('New Proj');
    });

    const req = httpTesting.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'New Proj', description: 'Desc' });
    req.flush(newProject);
  });

  it('should handle 409 conflict when creating duplicate project name', () => {
    service.create({ name: 'Duplicate Proj' }).subscribe({
      next: () => fail('Should have failed with 409'),
      error: (err) => {
        expect(err.status).toBe(409);
      },
    });

    const req = httpTesting.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Project already exists' }, { status: 409, statusText: 'Conflict' });
  });

  it('should update project details', () => {
    const updated: Project = { id: 1, name: 'Updated Proj', description: 'New Desc', active: true };

    service.update(1, { name: 'Updated Proj', description: 'New Desc' }).subscribe((res) => {
      expect(res.name).toBe('Updated Proj');
    });

    const req = httpTesting.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    req.flush(updated);
  });

  it('should set project active status', () => {
    const deactivated: Project = { id: 1, name: 'Proj', active: false };

    service.setStatus(1, false).subscribe((res) => {
      expect(res.active).toBeFalse();
    });

    const req = httpTesting.expectOne(`${baseUrl}/1/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
    req.flush(deactivated);
  });

  it('should manage project team member assignments', () => {
    const members: ProjectMember[] = [{ id: 10, fullName: 'Alice Member' }];

    service.getMembers(1).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].fullName).toBe('Alice Member');
    });
    const req1 = httpTesting.expectOne(`${baseUrl}/1/members`);
    expect(req1.request.method).toBe('GET');
    req1.flush(members);

    service.assignMember(1, 10).subscribe();
    const req2 = httpTesting.expectOne(`${baseUrl}/1/members`);
    expect(req2.request.method).toBe('POST');
    expect(req2.request.body).toEqual({ userId: 10 });
    req2.flush(null, { status: 201, statusText: 'Created' });

    service.removeMember(1, 10).subscribe();
    const req3 = httpTesting.expectOne(`${baseUrl}/1/members/10`);
    expect(req3.request.method).toBe('DELETE');
    req3.flush(null, { status: 204, statusText: 'No Content' });
  });
});
