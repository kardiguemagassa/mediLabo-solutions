export const server = 'http://localhost:8080';
export const authorizationserver = 'http://localhost:9001';
export const logoutUrl = 'http://localhost:9001/logout';


export const getFormData = (formValue: any, files: File[]): FormData => {
    const formData = new FormData();
    for (const property in formValue) {
        formData.append(property, formValue[property]);
    }
    if(files) { files.forEach(file => formData.append('files', file, file.name)); }
    return formData;
};