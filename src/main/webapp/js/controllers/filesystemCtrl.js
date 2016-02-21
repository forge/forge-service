angular.module('jboss-forge').controller(
		'filesystemCtrl',
		function($scope, $http, config) {
			// https://github.com/wix/angular-tree-control
			$scope.treeOptions = {
				nodeChildren : "children",
				dirSelectable : true,
				isLeaf : function(node) {
					return !node.container;
				},
				allowDeselect : false,
				injectClasses : {
					ul : "a1",
					li : "a2",
					liSelected : "a7",
					iExpanded : "a3",
					iCollapsed : "a4",
					iLeaf : "a5",
					label : "a6",
					labelSelected : "a8"
				}
			}
			$scope.selectResource = function(node) {
				$http.get(
						config.contextPath
								+ '/api/filesystem/contents?resource='
								+ node.path).success(function(data) {
					$scope.selectedResourceContents = data;
				})
				$scope.$emit('resourceChanged', node);
			}
			$http.get(config.contextPath + '/api/filesystem').success(
					function(data) {
						$scope.filesystem = [ data ];
					})

			$scope.downloadZipUrl = function(resource) {
				return config.contextPath + '/api/filesystem/zip?resource='
						+ resource;
			}
		});