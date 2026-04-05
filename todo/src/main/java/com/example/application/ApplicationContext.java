package com.example.application;

import java.util.List;

import com.example.MainApp;
import com.example.config.AppDataPaths;
import com.example.config.AppProperties;
import com.example.config.ConfigurationLoader;
import com.example.config.DatabaseProperties;
import com.example.config.JavaPreferencesStore;
import com.example.config.UserPreferencesStore;
import com.example.data.ConnectionFactory;
import com.example.data.JdbcConnectionFactory;
import com.example.data.JdbcScheduleRepository;
import com.example.data.ScheduleRepository;
import com.example.data.SchemaInitializer;
import com.example.data.SqliteConnectionFactory;
import com.example.data.SqliteMigrationRunner;
import com.example.data.SqliteScheduleRepository;
import com.example.databaseutil.ScheduleDAO;
import com.example.view.ScheduleCardStyleSupport;

public final class ApplicationContext {
    private final AppProperties appProperties;
    private final DatabaseProperties databaseProperties;
    private final AppDataPaths appDataPaths;
    private final UserPreferencesStore preferencesStore;
    private final ConnectionFactory connectionFactory;
    private final SchemaInitializer schemaInitializer;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final NavigationService navigationService;
    private final ThemeService themeService;
    private final MainViewModel mainViewModel;

    private ApplicationContext(
        AppProperties appProperties,
        DatabaseProperties databaseProperties,
        AppDataPaths appDataPaths,
        UserPreferencesStore preferencesStore,
        ConnectionFactory connectionFactory,
        SchemaInitializer schemaInitializer,
        ScheduleRepository scheduleRepository,
        ScheduleService scheduleService,
        NavigationService navigationService,
        ThemeService themeService,
        MainViewModel mainViewModel
    ) {
        this.appProperties = appProperties;
        this.databaseProperties = databaseProperties;
        this.appDataPaths = appDataPaths;
        this.preferencesStore = preferencesStore;
        this.connectionFactory = connectionFactory;
        this.schemaInitializer = schemaInitializer;
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
        this.navigationService = navigationService;
        this.themeService = themeService;
        this.mainViewModel = mainViewModel;
    }

    public static ApplicationContext createDefault() {
        AppProperties appProperties = ConfigurationLoader.loadAppProperties();
        DatabaseProperties databaseProperties = ConfigurationLoader.loadDatabaseProperties();
        UserPreferencesStore preferencesStore = new JavaPreferencesStore(MainApp.class);

        AppDataPaths appDataPaths = null;
        ConnectionFactory connectionFactory;
        SchemaInitializer schemaInitializer;
        ScheduleRepository scheduleRepository;
        if (databaseProperties.isSqliteMode()) {
            appDataPaths = new AppDataPaths(
                appProperties.getDataDirectoryOverride(),
                databaseProperties.getSqlitePath()
            );
            SqliteMigrationRunner migrationRunner = new SqliteMigrationRunner();
            connectionFactory = new SqliteConnectionFactory(databaseProperties, appDataPaths);
            schemaInitializer = null;
            scheduleRepository = new SqliteScheduleRepository(connectionFactory, migrationRunner);
        } else {
            connectionFactory = new JdbcConnectionFactory(databaseProperties);
            schemaInitializer = new SchemaInitializer();
            scheduleRepository = new JdbcScheduleRepository(new ScheduleDAO());
        }

        ScheduleService scheduleService = new ScheduleService(scheduleRepository);
        NavigationService navigationService = new NavigationService();
        ThemeService themeService = new ThemeService(
            preferencesStore,
            appProperties,
            List.copyOf(ScheduleCardStyleSupport.getStyleNames())
        );
        MainViewModel mainViewModel = new MainViewModel(navigationService, themeService);

        return new ApplicationContext(
            appProperties,
            databaseProperties,
            appDataPaths,
            preferencesStore,
            connectionFactory,
            schemaInitializer,
            scheduleRepository,
            scheduleService,
            navigationService,
            themeService,
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

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public SchemaInitializer getSchemaInitializer() {
        return schemaInitializer;
    }

    public ScheduleRepository getScheduleRepository() {
        return scheduleRepository;
    }

    public ScheduleService getScheduleService() {
        return scheduleService;
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public ThemeService getThemeService() {
        return themeService;
    }

    public MainViewModel getMainViewModel() {
        return mainViewModel;
    }
}
