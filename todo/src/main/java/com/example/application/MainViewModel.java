package com.example.application;

public final class MainViewModel {
    private final NavigationService navigationService;
    private final ThemeService themeService;

    public MainViewModel(NavigationService navigationService, ThemeService themeService) {
        this.navigationService = navigationService;
        this.themeService = themeService;
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public ThemeService getThemeService() {
        return themeService;
    }
}
