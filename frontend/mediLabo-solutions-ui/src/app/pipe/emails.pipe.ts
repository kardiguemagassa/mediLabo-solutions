import { Pipe, PipeTransform } from '@angular/core';
import { IMessage } from '../interface/message';

@Pipe({ name: 'Emails' })
export class Emails implements PipeTransform {
  transform(messages: IMessage[], args?: string[]): any {
    const emails = new Set();
    messages?.forEach(message => {
        emails.add(message.sender?.email === args[0] ? 'Vous' : message.sender?.email);
        emails.add(message.receiver?.email === args[0] ? 'Vous' : message.receiver?.email);
    });
    if(args.length == 1) {
        return [...emails];
    } else {
        return [...emails][0] === 'Vous' ? [...emails][1] : [...emails][0];
    }
  }
}