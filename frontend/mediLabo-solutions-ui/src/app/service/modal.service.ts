import { DestroyRef, inject, Injectable, ViewContainerRef } from '@angular/core';
import { Subject } from 'rxjs';
//import { ModalComponent } from '../component/navbar/modal/modal.component';

@Injectable()
export class ModalService {
    private modalNotifier: Subject<Event>;
    private readonly destroyRef = inject(DestroyRef);

    constructor() {}

    open = (viewRef: ViewContainerRef, options?: { message: string, title?: string, subtitle?: string, type?: 'success' | 'warning' | 'danger' }) => {
        //const modalComponent = viewRef.createComponent(ModalComponent);
        //modalComponent.setInput('message', options.message);
        //modalComponent.setInput('type', options.type);
        //modalComponent.setInput('subtitle', options.subtitle);
        //modalComponent.instance.closeEvent.subscribe(() => this.closeModal());
        //modalComponent.instance.submitEvent.subscribe((event) => this.submitModal(event));
        this.modalNotifier = new Subject();
        return this.modalNotifier.asObservable();
    };

    closeModal = () => this.modalNotifier.complete();

    submitModal = (event: Event) => {
        this.modalNotifier.next(event);
        this.closeModal();
    };
}