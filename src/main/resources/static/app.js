'use strict';

(function () {
    var MODULE = angular.module('efr', ['ngRoute', 'ngMaterial']);

    MODULE.run([function() {

    }]);

    MODULE.config(['$locationProvider', '$compileProvider', function($locationProvider, $compileProvider) {
        // hide index.html# in browser url
        $locationProvider.html5Mode({
            enabled: true
        });

        $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|ftp|mailto|tel|file|blob):/);
    }]);

    MODULE.controller('ViewController', ['$scope', '$routeParams', 'ViewService', '$location', '$timeout',
        function($scope, $routeParams, ViewService, $location, $timeout) {
            // flag if results are ready to present
            $scope.valid = false;
            // flag if error occurred
            $scope.error = false;
            // optional error message
            $scope.errorMessage = '';
            $scope.errorException = '';
            // process indicator
            $scope.processing = false;

            // chain to present
            $scope.chain = '1acj_A';
            $scope.searchText = null;

            // downloadable CSV content
            $scope.url = '';

            // NGL related
            var component;
            var highlight;

            // auto-complete user input
            $scope.querySearch = function querySearch(query) {
                if(query.length < 3) {
                    return [];
                }
                return ViewService.complete(query).then(function(response) {
                    return response.data;
                }, function(response) {
                    // case when backend works correctly and user input does not match any chain id
                    if(response.data.exception === 'NullPointerException') {
                        return [];
                    } else {
                        console.log(response.data);
                        $scope.errorException = /*response.data.exception*/'exception';
                        $scope.errorMessage = "failed to autocomplete by server: " + response.data.message;
                        $scope.error = true;
                    }
                });
            };

            // watch for valid change of selected chain, if so: auto-submit form and update view
            $scope.$watch('chain', function(newValue, oldValue) {
                if (newValue !== null && newValue.length === 6) {
                    console.log('updating view to ' + $scope.chain);
                    $scope.valid = false;
                    $scope.processing = true;
                    $scope.error = false;
                    ViewService.handleStructureQuery($scope.chain).then(function (response) {
                        $scope.protein = response.data;
                    }, function (response) {
                        console.log(response.data);
                        $scope.errorException = /*response.data.exception*/'exception';
                        $scope.errorMessage = response.data.message;
                        $scope.error = true;
                        $scope.processing = false;
                    });
                }
            });

            $scope.$watch('protein', function(newValue, oldValue) {
                if(newValue !== null && newValue !== undefined) {
                    $scope.valid = true;

                    var blob = new Blob([$scope.protein.csvRepresentation], { type : 'text/plain' });
                    $scope.url =  (window.URL || window.webkitURL).createObjectURL(blob);

                    // update NGL instance
                    $timeout(function() {
                        // ensure element is empty - e.g. when new structure is picked
                        angular.element(document.querySelector('#ngl')).empty();

                        var stage = new NGL.Stage('ngl', { backgroundColor : 'white' });
                        var stringBlob = new Blob([$scope.protein.pdbRepresentation], { type : 'text/plain' });

                        var processed = [];
                        $scope.protein.earlyFoldingResidues.forEach(function(earlyFoldingResidue) {
                            processed.push(earlyFoldingResidue.split("-")[1]);
                        });
                        var selection = processed.join(' or ');

                        var scheme = NGL.ColormakerRegistry.addSelectionScheme([
                            ["#176fc1", selection],
                            ["#cccccc", "*"]
                        ], "early folding residues");

                        stage.loadFile(stringBlob, { ext : 'pdb' }).then(function(comp) {
                            component = comp;

                            // draw actual structure
                            component.addRepresentation('cartoon', { color : scheme });
                            // adjust view
                            component.autoView();
                        });
                    }, 0);

                    $scope.processing = false;
                    $scope.error = false;
                }
            });

            $scope.select = function(residueIdentifier) {
                $scope.deselect();
                var selection = residueIdentifier.split("-")[1];
                highlight = component.addRepresentation('ball+stick', { name : 'highlight' })
                    .setSelection(selection);
            };

            $scope.deselect = function() {
                if(highlight) {
                    component.removeRepresentation(highlight);
                }
            };
        }]);

    MODULE.factory('ViewService', ['$http',
        function($http) {
            return {
                complete : function(query) {
                    return $http.get('efpred/api/complete/' + query);
                },
                handleStructureQuery : function(chain) {
                    return $http.get('efpred/api/id/' + chain);
                },
                submitFile : function(file) {
                    return $http({
                        url : 'efpred/api/submit',
                        method : 'POST',
                        data : { 'file': file },
                        headers : { 'Content-Type' : undefined }
                    });
                }
            }
        }]);

    // upload files as base64-encoded json query
    MODULE.directive('chooseFile', ['$location', 'ViewService', '$rootScope', function($location, ViewService, $rootScope) {
        return {
            scope : {
                templateUrl : "@"
            },
            link : function(scope, elem, attrs) {
                var parentScope = scope.$parent;
                var button = elem.find('button');
                var input = angular.element(elem[0].querySelector('input#fileInput'));
                button.bind('click', function() {
                    input[0].click();
                });
                input.bind('change', function() {
                    var fr = new FileReader();
                    fr.onload = function(event) {
                        console.log('updating view to uploaded structure');
                        parentScope.chain = '';
                        parentScope.valid = false;
                        parentScope.processing = true;
                        ViewService.submitFile(event.target.result).then(function(response) {
                            parentScope.protein = response.data;
                        }, function(response) {
                            console.log(response.data);
                            parentScope.errorException = /*response.data.exception*/'exception';
                            parentScope.errorMessage = response.data.message;
                            parentScope.error = true;
                            parentScope.processing = false;
                        });
                        parentScope.$apply();
                    };
                    fr.readAsDataURL(elem[0].querySelector('input#fileInput').files[0]);
                });
            }
        };
    }]);
})();