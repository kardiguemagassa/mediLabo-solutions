import { Pipe, PipeTransform } from '@angular/core';
import { IMessage } from '../interface/message';

@Pipe({ name: 'MessageGroup' })
export class MessageGroup implements PipeTransform {
  transform(messages: IMessage[], args: string[]): any {
    const emails = new Set();
    const groups = messages?.reduce((accumulator, message, index, array) => {
        emails.add(message.sender?.email);
        emails.add(message.receiver?.email);
        let key = message.conversationId;
        if (!accumulator[key]) {
            accumulator[key] = [];
        }
        accumulator[key].push(message);
        return accumulator;
    }, {});
    const data = [];
    for (const key in groups) {
        data.push({ conversationId: key, status: this.getEmails(groups[key], args[0])[2],  participants: (this.getEmails(groups[key], args[0])[0] as string[]).join(', '), images: this.getEmails(groups[key], args[0])[1], subject: groups[key][0].subject, messages: groups[key] });
    }
    console.log(data)
    return data;    
  }

  private getEmails = (messages: IMessage[], email: string) => {
    const emails = new Set();
    const images = new Set();
    const newMessageCount = [];
    messages.forEach(message => {
        images.add(message.sender?.imageUrl);
        emails.add(message.sender?.email === email ? 'Vous' : message.sender?.email);
        if (message.status === 'UNREAD') {
            newMessageCount.push(message.status);
        }
    });
    return [[...emails], [...images], newMessageCount.length > 0 ? 'UNREAD' : 'OPENED'];
};

}