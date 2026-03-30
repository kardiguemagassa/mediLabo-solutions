import { patchState, signalStore, watchState, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { tapResponse } from '@ngrx/operators';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { initialState, IState } from '../interface/state';
import { computed, inject } from '@angular/core';
import { getMessageCount } from '../utils/fileutils';
import { UserService } from '../service/user.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { pipe, switchMap, tap } from 'rxjs';
import { IResponse } from '../interface/response';
import { NotificationService } from '../service/notification.service';
import { IUser } from '../interface/user';
import { UpdatePassword } from '../interface/credentials';
import { AssessmentService } from '../service/assessment.service';
import { NoteService } from '../service/note.service';
import { PatientService } from '../service/patient.service';

export const AppStore = signalStore(
    { providedIn: 'root' },
    withState<IState>(initialState),
    // withWatch((state) todo something on state change),
    withComputed((store) => ({unreadMessageCount: computed(() => getMessageCount(store.messages()))})),

    // withHooks((store)
    withMethods((store, userService = inject(UserService), patientService = inject(PatientService), noteService = inject(NoteService), assessmentService = inject(AssessmentService), toastService = inject(HotToastService), notificationService = inject(NotificationService)) => ({
        
        // USERSERVICE
        getProfile: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.profile$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { profile: response.data.user, devices: response.data.devices, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        updateUser: rxMethod<IUser>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((user) => userService.update$(user).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        updatePassword: rxMethod<UpdatePassword>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((passwordRequest) => userService.updatePassword$(passwordRequest).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        toggleAccountLocked: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.toggleAccountLocked$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        toggleAccountExpired: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.toggleAccountExpired$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        toggleAccountEnabled: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.toggleAccountEnabled$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        enableMfa: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.enableMfa$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        disableMfa: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.disableMfa$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        updateRole: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((role) => userService.updateRole$(role).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        getUsers: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => userService.users$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ users: response.data.users, loading: false, error: null }));
                        //toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        getUser: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => userService.user$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ user: response.data.user, loading: false, error: null }));
                        //toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        updatePhoto: rxMethod<FormData>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((form) => userService.updateImage$(form).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ profile: response.data.user, loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        
        // ADMIN OPERATIONS ON OTHER USERS
        updateRoleByUuid: rxMethod<{ userUuid: string, role: string }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ userUuid, role }) => userService.updateRoleByUuid$(userUuid, role).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { user: response.data.user, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        toggleAccountLockedByUuid: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => userService.toggleAccountLockedByUuid$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { user: response.data.user, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        toggleAccountExpiredByUuid: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => userService.toggleAccountExpiredByUuid$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { user: response.data.user, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        toggleAccountEnabledByUuid: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => userService.toggleAccountEnabledByUuid$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { user: response.data.user, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        enableMfaByUuid: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => userService.enableMfaByUuid$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { user: response.data.user, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),
        disableMfaByUuid: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => userService.disableMfaByUuid$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { user: response.data.user, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                    }
                })
            )))),

        // PATIENTSSERVICE
        getAllPatients: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => patientService.allPatients$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { allPatients: response.data.patients, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getPatient: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((patientUuid) => patientService.patient$(patientUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ patient: response.data.patient, loading: false, error: null }));
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        createPatient: rxMethod<any>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((patient) => patientService.createPatient$(patient).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ allPatients: [...(state.allPatients ?? []), response.data.patient], loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getPatientByUserUuid: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((userUuid) => patientService.patientByUserUuid$(userUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { patient: response.data.patient, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getPatientByEmail: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((email) => patientService.patientByEmail$(email).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { patient: response.data.patient, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getMyPatientRecord: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => patientService.myPatientRecord$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { patient: response.data.patient, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        updatePatient: rxMethod<{ patientUuid: string, patient: any }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ patientUuid, patient }) => patientService.updatePatient$(patientUuid, patient).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            patient: response.data.patient,
                            allPatients: state.allPatients?.map(p => p.patientUuid === patientUuid ? response.data.patient : p) ?? [],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        deletePatient: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((patientUuid) => patientService.deletePatient$(patientUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            allPatients: state.allPatients?.filter(p => p.patientUuid !== patientUuid) ?? [],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        restorePatient: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((patientUuid) => patientService.restorePatient$(patientUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            allPatients: [...(state.allPatients ?? []), response.data.patient],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        
        // NOTESERVICE
        createNote: rxMethod<any>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((note) => noteService.createNote$(note).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            allNotes: [...(state.allNotes ?? []), response.data.note],
                            notes: [...(state.notes ?? []), response.data.note],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getAllNotes: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => noteService.allNotes$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { allNotes: response.data.notes, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getMyMedicalNotes: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => noteService.myMedicalNotes$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { notes: response.data.notes, loading: false, error: null });
                    },
                    error: (error: string) => {
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getNote: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((noteUuid) => noteService.note$(noteUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { noteDetail: response.data.note, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getNotesByPatient: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((patientUuid) => noteService.notesByPatient$(patientUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { notes: response.data.notes, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        updateNote: rxMethod<{ noteUuid: string, note: any }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ noteUuid, note }) => noteService.updateNote$(noteUuid, note).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            noteDetail: response.data.note,
                            notes: state.notes?.map(n => n.noteUuid === noteUuid ? response.data.note : n) ?? [],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        deleteNote: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((noteUuid) => noteService.deleteNote$(noteUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            notes: state.notes?.filter(n => n.noteUuid !== noteUuid) ?? [],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        // COMMENTS
        addComment: rxMethod<{ noteUuid: string, comment: any }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ noteUuid, comment }) => noteService.addComment$(noteUuid, comment).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            noteDetail: state.noteDetail ? { ...state.noteDetail, comments: [...(state.noteDetail.comments ?? []), response.data.comment] } : null,
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        updateComment: rxMethod<{ noteUuid: string, commentUuid: string, comment: any }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ noteUuid, commentUuid, comment }) => noteService.updateComment$(noteUuid, commentUuid, comment).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            noteDetail: state.noteDetail ? {
                                ...state.noteDetail,
                                comments: state.noteDetail.comments?.map(c => c.commentUuid === commentUuid ? response.data.comment : c) ?? []
                            } : null,
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        deleteComment: rxMethod<{ noteUuid: string, commentUuid: string }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ noteUuid, commentUuid }) => noteService.deleteComment$(noteUuid, commentUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            noteDetail: state.noteDetail ? {
                                ...state.noteDetail,
                                comments: state.noteDetail.comments?.filter(c => c.commentUuid !== commentUuid) ?? []
                            } : null,
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        // FILES
        uploadFile: rxMethod<{ noteUuid: string, file: FormData }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ noteUuid, file }) => noteService.uploadFile$(noteUuid, file).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            noteDetail: state.noteDetail ? {
                                ...state.noteDetail,
                                files: [...(state.noteDetail.files ?? []), response.data.file]
                            } : null,
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        deleteFile: rxMethod<{ noteUuid: string, fileUuid: string }>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(({ noteUuid, fileUuid }) => noteService.deleteFile$(noteUuid, fileUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            noteDetail: state.noteDetail ? {
                                ...state.noteDetail,
                                files: state.noteDetail.files?.filter(f => f.fileUuid !== fileUuid) ?? []
                            } : null,
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        
        // ASSESSMENTSSERVICE
        getAllAssessments: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => assessmentService.allAssessments$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { allAssessments: response.data.assessments, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
        )))),
        assessPatient: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((patientUuid) => assessmentService.assessPatient$(patientUuid).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({
                            assessmentDetail: { ...response.data.assessment, notes: '' },
                            allAssessments: [
                                ...(state.allAssessments?.filter(a => a.patientUuid !== patientUuid) ?? []),
                                response.data.assessment
                            ],
                            loading: false, error: null
                        }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),

        // NOTIFICATIONSERVICE
        getMessages: rxMethod<void>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap(() => notificationService.messages$().pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { messages: response.data.messages, loading: false, error: null });
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        sendMessage: rxMethod<any>(pipe(
            tap(() => patchState(store, { loading: true, error: null })),
            switchMap((form) => notificationService.sendMessage$(form).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { messages: response.data.messages, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        getConversation: rxMethod<string>(pipe(
            tap(() => patchState(store, { loading: true, error: null, conversation: null })),
            switchMap((conversationId) => notificationService.conversation$(conversationId).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, { conversation: response.data.conversation, messages: response.data.messages, loading: false, error: null });
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        replyToMessage: rxMethod<any>(pipe(
            tap(() => patchState(store, { error: null })),
            switchMap((form) => notificationService.replyToMessage$(form).pipe(
                tapResponse({
                    next: (response: IResponse) => {
                        patchState(store, (state) => ({ conversation: [...state.conversation, response.data.message], loading: false, error: null }));
                        toastService.success(response.message);
                    },
                    error: (error: string) => {
                        toastService.error(error ? error : `Une erreur s'est produite. Veuillez réessayer.`);
                        patchState(store, { loading: false, error });
                    }
                })
            )))),
        
        // OTHER METHODS
        setReportRequest(reportRequest: {}): void {
            patchState(store, (state) => ({ reportRequest }))
        },
        setCurrentPage(currentPage: number): void {
            patchState(store, (state) => ({ currentPage }))
        }
    })),
    withHooks({
        onInit(store) {
            watchState(store, (state) => {
                console.log('Current state', state);
            });
        }
    })
);