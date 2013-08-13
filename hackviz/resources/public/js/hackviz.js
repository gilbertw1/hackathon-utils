var createHourData = function(hourResults) {
    return [
        moment(hourResults.group, "YYYYMM").unix()*1000,
        hourResults.data[0].data[0]["repo-count"]
    ];
};

var createTeamData = function(teamResults) {
    return {
        name: teamResults.group,
        data: teamResults.data.map(createHourData)
    };
};

$(function () {
    $.getJSON('commits?metrics=repo:count&groups=team,day', function(data) {
        var series = data.map(createTeamData);
        console.log(series);
        $('#container').highcharts({
            chart: {
                zoomType: 'x',
                type: 'spline'
            },
            title: {
                text: 'Commits Over Time'
            },
            xAxis: {
                type: 'datetime'
            },
            yAxis: {
                title: {
                    text: 'Commits'
                }
            },
            tooltip: {
                formatter: function() {
                        return '<b>'+ this.series.name +'</b><br/>'+
                        Highcharts.dateFormat('%b', this.x) +': '+ this.y +' commits';
                }
            },
            series: series
        });
    });
});