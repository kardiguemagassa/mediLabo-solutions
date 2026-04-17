export interface IMessage {
    messageUuid: string;
    conversationId: string;
    subject: string;
    message: string;
    status: 'READ' | 'UNREAD';
    createdAt: string;
    updatedAt: string;
    sender: {
        userUuid: string;
        name: string;
        email: string;
        imageUrl?: string;
        role?: string;
    };
    receiver: {
        userUuid: string;
        name: string;
        email: string;
        imageUrl?: string;
        role?: string;
    };
}