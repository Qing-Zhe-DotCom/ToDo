package com.example.application;

public final class MainViewModel {
    private final NavigationService navigationService;
    private final ThemeService themeService;
    private final LocalizationService localizationService;
    private final FontService fontService;

    public MainViewModel(
        NavigationService navigationService,
        ThemeService themeService,
        LocalizationService localizationService,
        FontService fontService
    ) {
        this.navigationService = navigationService;
        this.themeService = themeService;
        this.localizationService = localizationService;
        this.fontService = fontService;
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public ThemeService getThemeService() {
        return themeService;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public FontService getFontService() {
        return fontService;
    }
}
