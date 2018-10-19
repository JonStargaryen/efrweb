'use strict';

(function () {
    var MODULE = angular.module('efr', ['ngRoute', 'ngMaterial']);

    MODULE.run([function() {

    }]);

    MODULE.controller('ViewController', ['$scope', '$routeParams', 'ViewService',
        function($scope, $routeParams, ViewService) {
            // flag if results are ready to present
            $scope.valid = false;
            // flag if error occurred
            $scope.error = false;
            // optional error message
            $scope.errorMessage = '';

            // chain to present
            $scope.chain = '1acj_A';
            $scope.searchText = null;

            // auto-complete user input
            $scope.querySearch = function querySearch(query) {
                return ViewService.complete(query);
            };

            // watch for valid change of selected chain, if so: auto-submit form
            $scope.$watch('chain', function (newValue, oldValue) {
                if (newValue !== null) {
                    console.log('updating view to ' + $scope.chain);
                    //TODO impl
                    // $location.path('/id/' + $scope.chain);
                }
            });

            ViewService.handleStructureQuery($scope.chain).then(function(response) {
                $scope.protein = response.data;
                $scope.valid = true;
            }, function(response) {
                $scope.errorMessage = response.data;
                $scope.error = true;
            });

        }]);

    MODULE.factory('ViewService', ['$http',
        function($http) {
            return {
                // handleLigandQuery : function(queryString) {
                //     return $http.get('/api/query/' + queryString);
                // },
                complete : function(query) {
                    return $http.get('/api/complete/' + query);
                },
                handleStructureQuery : function(chain) {
                    return $http.get('/api/id/' + chain);
                },
                submitFile : function(file) {
                    return $http({
                        url: apiUrl + 'submit',
                        method: 'POST',
                        data: {'file': file},
                        headers: {'Content-Type': undefined},
                        transformResponse: undefined
                    });
                }
            }
        }]);

    // upload files as base664-encoded json query
    MODULE.directive('chooseFile', ['$location', 'ViewService', function ($location, ViewService) {
        return {
            link: function (scope, elem, attrs) {
                //TODO impl
                // var button = elem.find('button');
                // var input = angular.element(elem[0].querySelector('input#fileInput'));
                // button.bind('click', function () {
                //     input[0].click();
                // });
                // input.bind('change', function () {
                //     var fr = new FileReader();
                //     fr.onload = function (event) {
                //         ViewService.submitFile(event.target.result).then(function (uuid) {
                //             // $location.path('/job/' + uuid);
                //         });
                //     };
                //     fr.readAsDataURL(elem[0].querySelector('input#fileInput').files[0]);
                // });
            }
        };
    }]);
})();