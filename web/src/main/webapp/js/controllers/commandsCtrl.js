angular.module('jboss-forge').controller(
		'commandsCtrl',
		function($scope, $rootScope, $http) {
			$scope.$watch('currentResource', function(currentResource) {
				if (currentResource) {
					$http.get(
							'/forge-service/api/forge/commands?resource='
									+ currentResource).success(function(data) {
						$scope.commands = data;
					});
				}
			});
		});