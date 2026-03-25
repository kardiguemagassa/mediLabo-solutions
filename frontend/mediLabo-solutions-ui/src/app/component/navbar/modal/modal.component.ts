// import { NgClass } from '@angular/common';
// import { Component, ElementRef, inject, input, output } from '@angular/core';


// @Component({
//   selector: 'app-modal',
//   imports: [NgClass],
//   templateUrl: './modal.component.html',
//   styleUrl: './modal.component.scss',
// })
// export class ModalComponent {

//   private elementRef = inject(ElementRef);
//   message = input<string>('');
//   subtitle = input<string>('');
//   type = input<'success' | 'warning' | 'danger'>('success');
//   closeEvent = output<void>();
//   submitEvent = output<Event>();

//   constructor() {}

//   close = () => {
//     this.elementRef.nativeElement.remove();
//     this.closeEvent.emit();
//   };

//   submit = (event: Event) => {
//     this.elementRef.nativeElement.remove();
//     this.submitEvent.emit(event);
//   };

// }
