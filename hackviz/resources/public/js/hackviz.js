var createHourData = function(hourResults) {
    return [
        moment(hourResults.group, "YYYYMMDD").unix()*1000,
        hourResults.data[0].data[0]["repo-count"]
    ];
};

var createTeamData = function(teamResults) {
    return {
        name: teamResults.group,
        data: teamResults.data.map(createHourData)
    };
};

var commitValue = function(commitData) {
    return commitData[1];
}

var createPieData = function(teamSeries) {
    return [
        teamSeries.name,
        teamSeries.data.map(commitValue).reduce(function(a,b) {return a+b})
    ];
};

$(function () {
    $.getJSON('commits?metrics=repo:count&groups=team,day', function(data) {
        var series = data.map(createTeamData);
        var pieSeries = series.map(createPieData);

        $('#spline').highcharts({
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
            series: series
        });

        $('#pie').highcharts({
            chart: {
                plotBackgroundColor: null,
                plotBorderWidth: null,
                plotShadow: false
            },
            title: {
                text: 'Total Commits'
            },
            tooltip: {
                pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    dataLabels: {
                        enabled: true,
                        color: '#000000',
                        connectorColor: '#000000',
                        format: '<b>{point.name}</b>: {point.percentage:.1f} %'
                    }
                }
            },
            series: [{
                type: 'pie',
                name: 'Commits',
                data: pieSeries
            }]
        });
    });
});