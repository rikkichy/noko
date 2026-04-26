# Contributing to Noko

## Translating Noko

Noko's locale strings live in `app/src/main/res/values/strings.xml`. To translate the app into your language:

1. Find your language's [Android locale qualifier](https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources). Examples:
   - Spanish → `values-es`
   - Brazilian Portuguese → `values-pt-rBR`
   - Japanese → `values-ja`
   - Simplified Chinese → `values-zh-rCN`
2. Create the folder `app/src/main/res/values-<your-lang>/` and copy `strings.xml` into it. (See `app/src/main/res/values-es/strings.xml` for an example.)
3. Translate the value of each `<string>` entry. **Do not** change the `name=` attribute, because that's the key the app looks up.
4. Inside `<xliff:g>` tags, leave the `%1$s` / `%2$s` placeholders untouched. Word order around them can be rearranged freely:
   ```xml
   <!-- English -->
   <string name="bubble_typing"><xliff:g id="character" example="NokoSama">%1$s</xliff:g> is typing</string>
   <!-- Japanese (placeholder moved, %1$s preserved) -->
   <string name="bubble_typing"><xliff:g id="character" example="NokoSama">%1$s</xliff:g>が入力中</string>
   ```
5. You don't need to translate everything at once, missing keys fall back to English automatically.
6. Open a PR with your `values-<lang>/strings.xml`. Please mention which keys you covered (or "all keys") in the PR description.

### Tips

- Apostrophes inside an English string are escaped (`\'`). If your language uses them too, escape them the same way.
- Keep ASCII typography (`...` not `…`) only where the original uses it; if your language conventionally uses different punctuation, use that instead.
- If a string doesn't make sense in your language without context, look at where it's referenced via `R.string.<name>` in the Kotlin source, or open an issue and ask.

### Verifying your translation

Run a debug build to see your strings on a device or emulator with the matching system locale:

```bash
./gradlew assembleDebug
```

> [!TIP]
> Don't build the release version. We have developer tools in debug builds that allow skipping onboarding and many other processes.

Then change your device language to the locale you translated. The app will pick up your strings automatically.
