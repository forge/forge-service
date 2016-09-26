//Add the necessary routes here
angular.module('jboss-forge', [ 'ui.router', 'treeControl']).run(
		[ '$rootScope', '$state', '$stateParams',
				function($rootScope, $state, $stateParams) {
					// It's very handy to add references to $state and
					// $stateParams to the $rootScope
					// so that you can access them from any scope within your
					// applications.For example,
					// <li ng-class="{ active: $state.includes('contacts.list')
					// }"> will set the <li>
					// to active whenever 'contacts.list' or one of its
					// descendents is active.
					$rootScope.$state = $state;
					$rootScope.$stateParams = $stateParams;
				} ]).config(
		function($stateProvider, $urlRouterProvider, $locationProvider) {
			// $locationProvider.html5Mode({
			// enabled : true,
			// requireBase : true
			// });
			$urlRouterProvider.otherwise('/');
			// Set up the states
			$stateProvider.state('home', {
				url : '/',
				templateUrl : 'views/home.html',
				controller : 'rootCtrl'
			}).state('wizard', {
				url : '/wizard/{wizardId}',
				templateUrl : 'views/wizard.html',
				controller : 'wizardCtrl'
			});
		})

.directive('stringToNumber', function() {
  return {
    require: 'ngModel',
    link: function(scope, element, attrs, ngModel) {
      ngModel.$parsers.push(function(value) {
        return '' + value;
      });
      ngModel.$formatters.push(function(value) {
        return parseFloat(value);
      });
    }
  };
});
