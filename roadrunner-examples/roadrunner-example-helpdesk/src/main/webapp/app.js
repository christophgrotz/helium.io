angular.module('helpdesk', [ 'roadrunner' ]).controller('HelpdeskCtrl', [ '$scope', '$timeout', 'angularRoadrunnerCollection', function($scope, $timeout, angularRoadrunnerCollection) {
	$scope.todos = angularRoadrunnerCollection(new Roadrunner('http://localhost:8080/helpdesk/todos/todos'));

	$scope.addTodo = function() {
		$scope.todos.add({
			text : $scope.todoText,
			done : false
		});
		$scope.todoText = '';
	};

	$scope.remaining = function() {
		var count = 0;
		angular.forEach($scope.todos, function(todo) {
			count += todo.done ? 0 : 1;
		});
		return count;
	};

	$scope.archive = function() {
		var oldTodos = $scope.todos;
		$scope.todos = [];
		angular.forEach(oldTodos, function(todo) {
			if (!todo.done)
				$scope.todos.push(todo);
		});
	};
} ]);