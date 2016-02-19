angular.module('jboss-forge').controller(
		'commandsCtrl',
		function($scope, $rootScope, $http, config) {
			$scope.$watch('currentResource', function(currentResource) {
				if (currentResource) {
					$http.get(
							config.contextPath + '/api/forge/commands?resource='
									+ currentResource).success(function(data) {
						$scope.commands = data;
					});
				}
			});
		});