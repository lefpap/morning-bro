import io.github.lefpap.morningbro.settings.Settings;
import io.github.lefpap.morningbro.settings.SettingsException;

void main() {
    Settings settings;
    try {
        settings = Settings.load(System.getenv());
    } catch (SettingsException e) {
        System.err.println(e.getMessage());
        System.exit(1);
        return;
    }

    IO.println(settings.summary());
}
