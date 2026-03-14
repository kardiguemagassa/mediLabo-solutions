export interface IMessage {
    messageId: number;
    messageUuid: string;
    conversationId: string;
    createdAt: string;
    message: string;
    receiverEmail: string;
    receiverFirstName: string;
    receiverImageUrl: string;
    receiverLastName: string;
    receiverUuid: string;
    senderEmail: string;
    senderFirstName: string;
    senderImageUrl: string;
    senderLastName: string;
    senderUuid: string;
    status: 'READ' | 'UNREAD';
    subject: string;
    updatedAt: string;
}