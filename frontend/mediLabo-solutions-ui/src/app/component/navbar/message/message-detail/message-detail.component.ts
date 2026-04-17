import { ChangeDetectionStrategy, ChangeDetectorRef, Component, effect, ElementRef, inject, input, Renderer2, viewChild } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { AppStore } from '../../../../store/app.store';
import { getFormData } from '../../../../utils/fileutils';
import { Emails } from '../../../../pipe/emails.pipe';

@Component({
  selector: 'app-message-detail',
  imports: [CommonModule, FormsModule, Emails],
  templateUrl: './message-detail.component.html',
  styleUrl: './message-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageDetailComponent {
  messageRef = viewChild<ElementRef<HTMLDivElement>>('message');
  conversationId = input<string>('');
  readonly store = inject(AppStore);
  private readonly location = inject(Location);
  private renderer = inject(Renderer2);
  private changeDetector = inject(ChangeDetectorRef);

  constructor() {
    effect(() => {
      this.store?.conversation();
      this.scrollChatWindow();
      this.changeDetector.markForCheck();
    });
  }

  ngAfterViewChecked () {
    this.scrollChatWindow();
  }

  ngOnInit(): void {
    if (this.conversationId()) {
      this.store.getConversation(this.conversationId());
    }
  }

  goBack = () => this.location.back();

  saveMessage = (form: NgForm) => {
    const conversation = this.store.conversation() ?? [];
    const myEmail = this.store.profile()?.email;
    let receiverEmail = '';
    for (const msg of conversation) {
        if (msg.sender?.email && msg.sender.email !== myEmail) { receiverEmail = msg.sender.email; break; }
        if (msg.receiver?.email && msg.receiver.email !== myEmail) { receiverEmail = msg.receiver.email; break; }
    }
    const payload = {
        receiverEmail,
        subject: 'Re: Conversation',
        message: form.value.message
    };
    console.log('Payload envoyé:', payload);
    this.store.replyToMessage(payload);
    form.reset();
  };

  private scrollChatWindow = () => {
    if(this.messageRef && this.messageRef()?.nativeElement && this.messageRef()?.nativeElement.scrollHeight > this.messageRef()?.nativeElement.scrollTop) {
      this.renderer.setProperty(this.messageRef()?.nativeElement, 'scrollTop', this.messageRef()?.nativeElement.scrollHeight);
    }
  };

}
