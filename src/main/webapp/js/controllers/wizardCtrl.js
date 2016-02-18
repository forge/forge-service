angular.module('jboss-forge')
		.controller(
				'wizardCtrl',
				function($scope, $rootScope, $state, $stateParams, $http) {
					var createPayload = function(model) {
						var inputs = [];
						for (att in model) {
							inputs.push({
								"name" : att,
								"value" : model[att] || ""
							});
						}
						return {
							'resource' : $scope.currentResource,
							stepIndex : $scope.navigationStack.length,
							inputs : inputs
						};
					}

					var createModel = function(model, data) {
						data.inputs.forEach(function(input) {
							model[input.name] = input.value;
						});
						return model;
					}

					// Model vars
					$scope.navigationStack = [];

					// Fetch the command info
					$http.get(
							'/api/forge/command/'
									+ $stateParams.wizardId + "?resource="
									+ $scope.currentResource).success(
							function(data) {
								$scope.wizard = data;
								$scope.wizardModel = createModel({}, data);
							});

					// Watch for model changes
					$scope.$watchCollection('wizardModel', function(model) {
						if (model == null || $scope.wizardForm.$pristine)
							return;
						var payload = createPayload(model);
						$http.post(
								'/api/forge/command/'
										+ $stateParams.wizardId + '/validate',
								payload).success(function(data) {
							$scope.wizard.state = data.state;
							$scope.wizard.messages = data.messages;
						})
					});

					// Called when next page button is pressed
					$scope.nextPage = function(wizard, model, navigationStack) {
						navigationStack.push(angular.copy(wizard));
						var payload = createPayload(model);
						$http.post(
								'/api/forge/command/'
										+ $stateParams.wizardId + '/next',
								payload).success(function(data) {
							$scope.wizard.state = data.state;
							$scope.wizard.metadata = data.metadata;
							$scope.wizard.inputs = data.inputs;
							createModel(model, data);
						})

					}

					// Called when previous page button is pressed
					$scope.previousPage = function(wizard, wizardModel,
							navigationStack) {
						var previousWizardPage = navigationStack.pop();
						console.log(previousWizardPage);
						$scope.wizard = previousWizardPage;
					}

					// Called when cancel button is pressed
					$scope.cancel = function() {
						$state.go('home', {}, {
							location : 'replace'
						});
					}

					// Called when finish button is pressed
					$scope.finish = function(model) {
						var payload = createPayload(model);
						$http.post(
								'/api/forge/command/'
										+ $stateParams.wizardId + '/execute',
								payload).success(function(data) {
							$scope.wizard.results = data.results;
							$scope.wizard.messages = data.messages;
							$scope.wizard.err = data.err;
							$scope.wizard.out = data.out;

						});
					}

				});