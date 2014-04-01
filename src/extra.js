function isEmpty(str) {
	return (!str || 0 === str.length);
}

function toString(string) {
	if (isEmpty(string)) {
		return '';
	}
	return '' + string;
}