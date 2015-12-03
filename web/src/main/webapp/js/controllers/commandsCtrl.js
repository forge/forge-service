angular.module('jboss-forge').controller(
		'commandsCtrl',
		function($scope, $rootScope, $http) {
			$http.get('/forge-service/api/forge/commands?resource=/tmp').success(
					function(data) {
						$scope.commands = data;
					})
		});