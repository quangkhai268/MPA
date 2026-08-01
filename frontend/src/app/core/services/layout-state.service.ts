import { Injectable, signal } from '@angular/core';

/** Trạng thái sidebar drawer trên mobile — chia sẻ giữa HeaderComponent (nút hamburger)
 * và SidebarComponent (drawer), 2 component anh em dưới MainLayoutComponent. */
@Injectable({ providedIn: 'root' })
export class LayoutStateService {
  mobileNavOpen = signal(false);

  toggle(): void { this.mobileNavOpen.update(v => !v); }
  close(): void { this.mobileNavOpen.set(false); }
}
