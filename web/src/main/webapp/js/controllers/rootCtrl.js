angular.module('jboss-forge').controller('rootCtrl', function($scope) {
	// Listens for change on filesystemCtrl
	$scope.currentResource = "";
	$scope.$on('resourceChanged', function(event, data) {
		$scope.currentResource = data.path || "";
	});
});