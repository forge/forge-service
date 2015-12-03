angular.module('jboss-forge').controller(
		'wizardCtrl',
		function($scope, $state, $stateParams, $http) {
			var createPayload = function(model) {
				var inputs = [];
				for (att in model) {
					inputs.push({
						"name" : att,
						"value" : model[att] || ""
					});
				}
				return {
					'resource' : '/tmp',
					inputs : inputs
				};
			}

			var createModel = function(data) {
				// Initialize model
				var model = {};
				data.inputs.forEach(function(input) {
					model[input.name] = input.value;
				});
				return model;
			}

			$http
					.get(
							'/forge-service/api/forge/command/'
									+ $stateParams.wizardId).success(
							function(data) {
								$scope.wizard = data;
								$scope.wizardModel = createModel(data);
							});
			// Watch for model changes
			$scope.$watchCollection('wizardModel', function(model) {
				if (model == null)
					return;
				var payload = createPayload(model);
				$http.post(
						'/forge-service/api/forge/command/'
								+ $stateParams.wizardId + '/validate', payload)
						.success(function(data) {
							$scope.wizard = data;
							$scope.wizardModel = createModel(data);
						})
			});
			$scope.nextPage = function() {
			}
			$scope.previousPage = function() {
			}
			$scope.cancel = function() {
				$state.go('home', {}, {
					location : 'replace'
				});
			}

			$scope.finish = function(model) {
				var payload = createPayload(model);
				$http.post(
						'/forge-service/api/forge/command/'
								+ $stateParams.wizardId + '/execute', payload)
						.success(function(data) {
							console.log(data);
						});
			}

		});