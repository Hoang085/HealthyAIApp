package com.healthyai.app.domain.usecase.health;

import com.healthyai.app.domain.repository.HabitRepository;
import com.healthyai.app.domain.repository.HealthDataRepository;
import com.healthyai.app.domain.usecase.HabitRuleEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class GetHealthInsightsUseCase_Factory implements Factory<GetHealthInsightsUseCase> {
  private final Provider<HabitRepository> habitRepositoryProvider;

  private final Provider<HealthDataRepository> healthDataRepositoryProvider;

  private final Provider<HabitRuleEngine> ruleEngineProvider;

  public GetHealthInsightsUseCase_Factory(Provider<HabitRepository> habitRepositoryProvider,
      Provider<HealthDataRepository> healthDataRepositoryProvider,
      Provider<HabitRuleEngine> ruleEngineProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
    this.healthDataRepositoryProvider = healthDataRepositoryProvider;
    this.ruleEngineProvider = ruleEngineProvider;
  }

  @Override
  public GetHealthInsightsUseCase get() {
    return newInstance(habitRepositoryProvider.get(), healthDataRepositoryProvider.get(), ruleEngineProvider.get());
  }

  public static GetHealthInsightsUseCase_Factory create(
      Provider<HabitRepository> habitRepositoryProvider,
      Provider<HealthDataRepository> healthDataRepositoryProvider,
      Provider<HabitRuleEngine> ruleEngineProvider) {
    return new GetHealthInsightsUseCase_Factory(habitRepositoryProvider, healthDataRepositoryProvider, ruleEngineProvider);
  }

  public static GetHealthInsightsUseCase newInstance(HabitRepository habitRepository,
      HealthDataRepository healthDataRepository, HabitRuleEngine ruleEngine) {
    return new GetHealthInsightsUseCase(habitRepository, healthDataRepository, ruleEngine);
  }
}
