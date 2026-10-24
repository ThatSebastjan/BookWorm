This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Project structure

BookWorm has two apps on one shared core:

| Module | What it is |
|---|---|
| `androidApp`, `iosApp` | Entry points of the **student app**. The UI itself lives in `shared`. |
| `desktopApp` | The **teacher app**: its screens, components and strings. |
| `shared` | Everything both apps use: domain models, repository interfaces, the fake backend, the BookWorm design system, and the student screens. |

`shared/src/commonMain/kotlin/com/dreamteam/bookworm/`:

- `domain/model` – data classes both apps work with (chapters, lessons, questions, classes, learning paths).
- `domain/repository` – the interfaces the apps call. Implement these against the backend.
- `data/fake` – `FakeBackend` and `FakeAuthRepository`, in-memory implementations with sample content.
  One `FakeBackend` serves both apps, so what a teacher changes shows up for students.
- `di/AppContainer.kt` – picks the implementations. Swap `AppContainer.fake()` for real repositories here.
- `screens/<feature>` – student screens, one screen plus its ViewModel per feature.
- `ui_components` – the BookWorm design system (theme, buttons, cards, inputs, dialogs).
- `composeResources/values*/strings.xml` – student UI text in English and Slovenian (`values-sl`).

`desktopApp/src/main/`:

- `kotlin/.../main.kt` – opens the teacher app window.
- `kotlin/.../teacher/TeacherApp.kt` – the sidebar shell and class switcher.
- `kotlin/.../teacher/screens/<feature>` – teacher screens, one screen plus its ViewModel per feature
  (materials, review, curriculum, lessoneditor, classroom, quizsetup, live, reveal).
- `kotlin/.../teacher/components` – pieces only the teacher app uses (page layout, form dialog, dropdown,
  teacher colours). Everything generic comes from `shared/ui_components`.
- `kotlin/.../teacher/platform` – file picking, drag and drop, clipboard, printing and sound on desktop.
- `composeResources/values*/strings.xml` – teacher UI text in English and Slovenian.

Questions arrive without their correct answers. The app sends the student's answer with
`LessonRepository.submitAnswer` and shows the result the backend returns.
See [docs/backend-integration.md](docs/backend-integration.md) for how to connect a real backend.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…