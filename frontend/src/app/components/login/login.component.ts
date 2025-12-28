import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error: string | null = null;
  returnUrl = '/dashboard';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Redirect if already logged in
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }

    // Get return URL from query params
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.error = 'Bitte Benutzername und Passwort eingeben';
      return;
    }

    this.loading = true;
    this.error = null;

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.router.navigate([this.returnUrl]);
      },
      error: (err) => {
        console.error('Login error:', err);
        this.loading = false;
        if (err.status === 401) {
          this.error = 'Ungueltige Anmeldedaten';
        } else {
          this.error = 'Anmeldung fehlgeschlagen. Bitte versuchen Sie es erneut.';
        }
      },
    });
  }
}
