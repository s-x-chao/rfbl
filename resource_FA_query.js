db.getCollection('resource').aggregate( [ { $unwind: "$ranks" } , {$match : {'ranks.free_agent' : true}}, {$project : {'ranks.fullname' : 1, 'ranks.}}] )