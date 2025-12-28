import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { KlientListComponent } from './components/klient-list/klient-list.component';
import { KlientFormComponent } from './components/klient-form/klient-form.component';
import { LoginComponent } from './components/login/login.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'klienten', component: KlientListComponent, canActivate: [authGuard] },
  { path: 'klienten/neu', component: KlientFormComponent, canActivate: [authGuard] },
  { path: 'klienten/:id', component: KlientFormComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '/dashboard' },
];
