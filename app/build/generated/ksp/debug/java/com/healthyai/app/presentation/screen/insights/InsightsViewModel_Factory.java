package com.healthyai.app.presentation.screen.insights;

import com.healthyai.app.domain.repository.HabitRepository;
import com.healthyai.app.domain.usecase.GenerateSuggestionsUseCase;
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
public final class InsightsViewModel_Factory implements Factory<InsightsViewModel> {
  private final Provider<HabitRepository> habitRepositoryProvider;

  private final Provider<GenerateSuggestionsUseCase> generateSuggestionsUseCaseProvider;

  private final Provider<HabitRuleEngine> ruleEngineProvider;

  public InsightsViewModel_Factory(Provider<HabitRepository> habitRepositoryProvider,
      Provider<GenerateSuggestionsUseCase> generateSuggestionsUseCaseProvider,
      Provider<HabitRuleEngine> ruleEngineProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
    this.generateSuggestionsUseCaseProvider = generateSuggestionsUseCaseProvider;
    this.ruleEngineProvider = ruleEngineProvider;
  }

  @Override
  public InsightsViewModel get() {
    return newInstance(habitRepositoryProvider.get(), generateSuggestionsUseCaseProvider.get(), ruleEngineProvider.get());
  }

  public static InsightsViewModel_Factory create(Provider<HabitRepository> habitRepositoryProvider,
      Provider<GenerateSuggestionsUseCase> generateSuggestionsUseCaseProvider,
      Provider<HabitRuleEngine> ruleEngineProvider) {
    return new InsightsViewModel_Factory(habitRepositoryProvider, generateSuggestionsUseCaseProvider, ruleEngineProvider);
  }

  public static InsightsViewModel newInstance(HabitRepository habitRepository,
      GenerateSuggestionsUseCase generateSuggestionsUseCase, HabitRuleEngine ruleEngine) {
    return new InsightsViewModel(habitRepository, generateSuggestionsUseCase, ruleEngine);
  }
}
