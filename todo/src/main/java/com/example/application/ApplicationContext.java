package com.example.application;

import com.example.MainApp;
import com.example.config.AppDataPaths;
import com.example.config.AppProperties;
import com.example.config.ConfigurationLoader;
import com.example.config.DatabaseProperties;
import com.example.config.JavaPreferencesStore;
import com.example.config.UserPreferencesStore;
import com.example.data.ConnectionFactory;
import com.example.data.JdbcConnectionFactory;
import com.example.data.MysqlStageBSchemaManager;
import com.example.data.ScheduleItemRepository;
import com.example.data.SchemaManager;
import com.example.data.SqlDialect;
import com.example.data.SqlScheduleItemRepository;
import com.example.data.SqliteConnectionFactory;
import com.example.data.SqliteStageBSchemaManager;
public final class ApplicationContext {
    private final AppProperties appProperties;
    private final DatabaseProperties databaseProperties;
    private final AppDataPaths appDataPaths;
    private final UserPreferencesStore preferencesStore;
    private final CustomOptionsService customOptionsService;
    private final ConnectionFactory connectionFactory;
    private final SchemaManager schemaManager;
    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleItemService scheduleItemService;
    private final NavigationService navigationService;
    private final ExperimentalFeaturesService experimentalFeaturesService;
    private final ThemeService themeService;
    private final IconService iconService;
    private final LocalizationService localizationService;
    private final FontService fontService;
    private final MainViewModel mainViewModel;

    private ApplicationContext(
        AppProperties appProperties,
        DatabaseProperties databaseProperties,
        AppDataPaths appDataPaths,
        UserPreferencesStore preferencesStore,
        CustomOptionsService customOptionsService,
        ConnectionFactory connectionFactory,
        SchemaManager schemaManager,
        ScheduleItemRepository scheduleItemRepository,
        ScheduleItemService scheduleItemService,
        NavigationService navigationService,
        ExperimentalFeaturesService experimentalFeaturesService,
        ThemeService themeService,
        IconService iconService,
        LocalizationService localizationService,
        FontService fontService,
        MainViewModel mainViewModel
    ) {
        this.appProperties = appProperties;
        this.databaseProperties = databaseProperties;
        this.appDataPaths = appDataPaths;
        this.preferencesStore = preferencesStore;
        this.customOptionsService = customOptionsService;
        this.connectionFactory = connectionFactory;
        this.schemaManager = schemaManager;
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleItemService = scheduleItemService;
        this.navigationService = navigationService;
        this.experimentalFeaturesService = experimentalFeaturesService;
        this.themeService = themeService;
        this.iconService = iconService;
        this.localizationService = localizationService;
        this.fontService = fontService;
        this.mainViewModel = mainViewModel;
    }

    public static ApplicationContext createDefault() {
        AppProperties appProperties = ConfigurationLoader.loadAppProperties();
        DatabaseProperties databaseProperties = ConfigurationLoader.loadDatabaseProperties();
        UserPreferencesStore preferencesStore = new JavaPreferencesStore(MainApp.class);
        LocalizationService localizationService = new LocalizationService(
            preferencesStore,
            appProperties.getDefaultLanguage()
        );
        FontService fontService = new FontService(preferencesStore);
        ExperimentalFeaturesService experimentalFeaturesService = new ExperimentalFeaturesService(preferencesStore);
        CustomOptionsService customOptionsService = new CustomOptionsService(preferencesStore);

        AppDataPaths appDataPaths = null;
        ConnectionFactory connectionFactory;
        SchemaManager schemaManager;
        ScheduleItemRepository scheduleItemRepository;
        if (databaseProperties.isSqliteMode()) {
            appDataPaths = new AppDataPaths(
                appProperties.getDataDirectoryOverride(),
                databaseProperties.getSqlitePath()
            );
            connectionFactory = new SqliteConnectionFactory(databaseProperties, appDataPaths);
            schemaManager = new SqliteStageBSchemaManager();
            scheduleItemRepository = new SqlScheduleItemRepository(connectionFactory, schemaManager, SqlDialect.SQLITE);
        } else {
            connectionFactory = new JdbcConnectionFactory(databaseProperties);
            schemaManager = new MysqlStageBSchemaManager();
            scheduleItemRepository = new SqlScheduleItemRepository(connectionFactory, schemaManager, SqlDialect.MYSQL);
        }

        ScheduleItemService scheduleItemService = new ScheduleItemService(
            scheduleItemRepository,
            preferencesStore,
            appProperties.getAppVersion()
        );
        try {
            scheduleItemService.initializeRuntime();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize stage-B data runtime", exception);
        }
        NavigationService navigationService = new NavigationService();
        ThemeService themeService = new ThemeService(preferencesStore, appProperties, experimentalFeaturesService);
        IconService iconService = new IconService(preferencesStore, themeService.getCurrentThemeFamily());
        iconService.syncThemeAppearance(themeService.getCurrentAppearance());
        MainViewModel mainViewModel = new MainViewModel(
            navigationService,
            themeService,
            iconService,
            localizationService,
            fontService
        );

        return new ApplicationContext(
            appProperties,
            databaseProperties,
            appDataPaths,
            preferencesStore,
            customOptionsService,
            connectionFactory,
            schemaManager,
            scheduleItemRepository,
            scheduleItemService,
            navigationService,
            experimentalFeaturesService,
            themeService,
            iconService,
            localizationService,
            fontService,
            mainViewModel
        );
    }

    public AppProperties getAppProperties() {
        return appProperties;
    }

    public DatabaseProperties getDatabaseProperties() {
        return databaseProperties;
    }

    public AppDataPaths getAppDataPaths() {
        return appDataPaths;
    }

    public UserPreferencesStore getPreferencesStore() {
        return preferencesStore;
    }

    public CustomOptionsService getCustomOptionsService() {
        return customOptionsService;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public SchemaManager getSchemaManager() {
        return schemaManager;
    }

    public ScheduleItemRepository getScheduleItemRepository() {
        return scheduleItemRepository;
    }

    public ScheduleItemService getScheduleItemService() {
        return scheduleItemService;
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public ThemeService getThemeService() {
        return themeService;
    }

    public IconService getIconService() {
        return iconService;
    }

    public ExperimentalFeaturesService getExperimentalFeaturesService() {
        return experimentalFeaturesService;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public FontService getFontService() {
        return fontService;
    }

    public MainViewModel getMainViewModel() {
        return mainViewModel;
    }
}
