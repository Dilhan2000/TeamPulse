import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { ProjectManagementComponent } from './project-management.component';
import { ManagerProjectService } from '../../../core/services/manager-project.service';
import { Project } from '../../../core/models/project.model';
import { PageResponse } from '../../../core/models/user.model';

describe('ProjectManagementComponent', () => {
  let component: ProjectManagementComponent;
  let fixture: ComponentFixture<ProjectManagementComponent>;
  let managerProjectServiceSpy: jasmine.SpyObj<ManagerProjectService>;
  let dialog: MatDialog;

  const mockProjects: Project[] = [
    { id: 1, name: 'Project Alpha', description: 'Alpha Desc', active: true },
    { id: 2, name: 'Project Beta', description: 'Beta Desc', active: false },
  ];

  const mockPageResponse: PageResponse<Project> = {
    content: mockProjects,
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  beforeEach(async () => {
    managerProjectServiceSpy = jasmine.createSpyObj('ManagerProjectService', [
      'list',
      'setStatus',
    ]);

    managerProjectServiceSpy.list.and.returnValue(of(mockPageResponse));
    managerProjectServiceSpy.setStatus.and.returnValue(
      of({ id: 1, name: 'Project Alpha', description: 'Alpha Desc', active: false })
    );

    await TestBed.configureTestingModule({
      imports: [ProjectManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ManagerProjectService, useValue: managerProjectServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {},
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectManagementComponent);
    component = fixture.componentInstance;
    dialog = fixture.debugElement.injector.get(MatDialog);
  });

  it('should create component and load projects on init', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(managerProjectServiceSpy.list).toHaveBeenCalled();
    expect(component.projects().length).toBe(2);
    expect(component.totalElements()).toBe(2);
  });

  it('should toggle active status and update row immediately', () => {
    fixture.detectChanges();

    const proj = component.projects()[0];
    component.onToggleStatus(proj, false);

    expect(managerProjectServiceSpy.setStatus).toHaveBeenCalledWith(1, false);
    expect(proj.active).toBeFalse();
  });

  it('should filter by search query on search change', () => {
    fixture.detectChanges();

    component.searchQuery = 'Alpha';
    component.onSearchChange();

    expect(component.pageIndex()).toBe(0);
    expect(managerProjectServiceSpy.list).toHaveBeenCalledWith(
      jasmine.objectContaining({ search: 'Alpha' })
    );
  });

  it('should filter by status on status filter change', () => {
    fixture.detectChanges();

    component.statusFilter = 'ACTIVE';
    component.onStatusFilterChange();

    expect(component.pageIndex()).toBe(0);
    expect(managerProjectServiceSpy.list).toHaveBeenCalledWith(
      jasmine.objectContaining({ active: true })
    );
  });

  it('should open create dialog when Add Project is called', () => {
    const spy = spyOn(dialog, 'open').and.returnValue({
      afterClosed: () => of(null),
    } as any);

    component.openCreateDialog();
    expect(spy).toHaveBeenCalled();
  });

  it('should open edit dialog with project data', () => {
    const spy = spyOn(dialog, 'open').and.returnValue({
      afterClosed: () => of(null),
    } as any);

    component.openEditDialog(mockProjects[0]);
    expect(spy).toHaveBeenCalledWith(
      jasmine.any(Function),
      jasmine.objectContaining({ data: { project: mockProjects[0] } })
    );
  });
});
