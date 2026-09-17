package com.healthyai.app.presentation.screen.insights;

import com.healthyai.app.domain.usecase.health.GetHealthInsightsUseCase;
import com.healthyai.app.domain.usecase.health.GetVitalityScoreUseCase;
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
  private final Provider<GetHealthInsightsUseCase> getHealthInsightsUseCaseProvider;

  private final Provider<GetVitalityScoreUseCase> getVitalityScoreUseCaseProvider;

  public InsightsViewModel_Factory(
      Provider<GetHealthInsightsUseCase> getHealthInsightsUseCaseProvider,
      Provider<GetVitalityScoreUseCase> getVitalityScoreUseCaseProvider) {
    this.getHealthInsightsUseCaseProvider = getHealthInsightsUseCaseProvider;
    this.getVitalityScoreUseCaseProvider = getVitalityScoreUseCaseProvider;
  }

  @Override
  public InsightsViewModel get() {
    return newInstance(getHealthInsightsUseCaseProvider.get(), getVitalityScoreUseCaseProvider.get());
  }

  public static InsightsViewModel_Factory create(
      Provider<GetHealthInsightsUseCase> getHealthInsightsUseCaseProvider,
      Provider<GetVitalityScoreUseCase> getVitalityScoreUseCaseProvider) {
    return new InsightsViewModel_Factory(getHealthInsightsUseCaseProvider, getVitalityScoreUseCaseProvider);
  }

  public static InsightsViewModel newInstance(GetHealthInsightsUseCase getHealthInsightsUseCase,
      GetVitalityScoreUseCase getVitalityScoreUseCase) {
    return new InsightsViewModel(getHealthInsightsUseCase, getVitalityScoreUseCase);
  }
}
