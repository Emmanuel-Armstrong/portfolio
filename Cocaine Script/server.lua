local playersInField, playersInLab, playerQueue, cooldown = {}, {}, {}, {}
local playerWithDropOff = nil
local openWorldBarrels = {}
local openWorldBarrelsForClients = {}
local openWorldFields = {}
local openWorldFieldsForClients = {}
local cokefield = {
    [1] = {},
    [2] = {},
    [3] = {},
    [4] = {},
}
local fieldCounts = {
    [1] = 0,
    [2] = 0,
    [3] = 0,
    [4] = 0,
}
local finishcashdropoff = false

Citizen.CreateThread(function()
    while TMC == nil do
        Citizen.Wait(1000)
    end
end)

TMC.Functions.RegisterServerEvent('drugs:cokesenddroplocation', function(src, area)
    finishcashdropoff = false
    local player = TMC.Functions.GetPlayer(src)
    local phoneNumber = player.PlayerData.charinfo.phone
    local sender = 'Unknown'
    local subject = 'Dead Drop'
    local message = 'Hey. <br><br>Get the rolls over to one of my dead drops, it should be around the ' .. Config.Cocaine.DropOffLocations[area].area_name .. ' area. <br><br>I\'ve set a GPS marker to around the area. Just have a look and we will be in touch!'
    exports['tmc_phone']:SendEmail(phoneNumber, sender, subject, message)
end)

TMC.Functions.RegisterServerEvent('drugs:cokesendharvestorder', function(src, field)
    local player = TMC.Functions.GetPlayer(src)
    local phoneNumber = player.PlayerData.charinfo.phone
    local sender = 'Unknown'
    local subject = 'Harvesting Order'
    local message = 'Hey. <br><br>I got word that a Field is ready to harvest. <br><br>Go over to Cayo Perico and make sure you bring the right equipment. <br><br>The right field should be Field #' .. field .. '. <br><br>I wouldn\'t touch the other fields, otherwise some people might not be so happy!'
    exports['tmc_phone']:SendEmail(phoneNumber, sender, subject, message)
end)

TMC.Functions.RegisterServerEvent('drugs:cokequeue', function(src, status)
    if status == 'joinqueue' then

        if not playerWithDropOff then
            playerWithDropOff = src
        end

        local allFieldsFull = true
        for k,v in pairs(openWorldFields) do
            --print(v.inUse)
            if v.inUse == false and v.frozen == false then
                allFieldsFull = false
                break
            end
        end


        Citizen.Wait(500)
        --print("player with drop off: ", playerWithDropOff)
        if (playerWithDropOff and playerWithDropOff ~= src) or allFieldsFull == true then
            --print('inserting into playerqueue dropoff or field not nil')
            table.insert(playerQueue, src)
            if allFieldsFull == true then
                TMC.Functions.SimpleNotify(src, "All Fields are currently claimed. You're in the system so just hang out and we\'ll get you sorted once one is open.", "info", 10000)
            end
        else
            --print('no queue, sending dropoff')
            playerWithDropOff = src
            cashBinTimer(src)
            TriggerClientEvent('drugs:cokesendDropOff', src)
        end
    elseif status == 'leavequeue' then
        --print("in leavequeue")
        if playerWithDropOff == src then
            playerWithDropOff = nil
        end

        if tablefind(playerQueue, src) then
            --print("found player in queue table in leave queue")
            table.remove(playerQueue, tablefind(playerQueue, src))
            finishcashdropoff = false
            TriggerClientEvent('drugs:cokedropofffail', src)
        else
            finishcashdropoff = false
            TriggerClientEvent('drugs:cokedropofffail', src)
        end
        if playerWithDropOff == nil then
            findNextInQueue()
        end
    end
end)

TMC.Functions.RegisterServerEvent('drugs:cokegivefield', function(src, player, status)
    --print("status", status)
    local playerId = TMC.Functions.GetPlayer(src).PlayerData.citizenid
    if status then
        Citizen.Wait(200)
        finishcashdropoff = true
        local fieldRandom = TMC.Common.TrueRandom(1,4)
        local foundField = false
        while foundField == false do
            if openWorldFields[fieldRandom].inUse == false and openWorldFields[fieldRandom].frozen == false then
                openWorldFields[fieldRandom].inUse = true
                openWorldFields[fieldRandom].player = playerId
                openWorldFields[fieldRandom].wasEntered = false
                openWorldFields[fieldRandom].timer = 0
                openWorldFields[fieldRandom].runTimer = true
                foundField = true
            else
                fieldRandom = TMC.Common.TrueRandom(1,4)
            end
            Citizen.Wait(200)
        end
        --print('player assigned field:', openWorldFields[fieldRandom].player)
        --print('field assigned', fieldRandom)

        playerWithDropOff = nil
        TriggerClientEvent('drugs:cokefieldsendinfo', src, fieldRandom, 140000)
        --TriggerClientEvent('drugs:cokefieldsendinfo', src, fieldRandom, 1000) -- USED FOR DEV TESTING
        for k,v in pairs(openWorldFields) do
            if v.inUse == false and v.frozen == false then
                findNextInQueue()
                break
            end
        end
        fieldRandom = nil
    elseif status == false then
        local field = nil
        for k,v in pairs(openWorldFields) do
            if v.player == playerId or v.player == src then
                openWorldFields[k].inUse = false
                openWorldFields[k].wasEntered = false
                openWorldFields[k].timer = 0
                openWorldFields[k].runTimer = false
                openWorldFields[fieldRandom].player = nil
                Citizen.Wait(200)
                --print("should be false :", openWorldFields[k].inUse)
                --print('k,v', k, v)
                field = k
            end
        end
        startFieldFrozenTimer(field)
    end
end)

--Coke Field Code NEW
--------------------------------------------------------------------------------------------------------------------------------------------------------
Citizen.CreateThread(function()
    GlobalState.openWorldFieldsReady = false
    --print("Should select from cokefields")
    TMC.Functions.ExecuteSqlSync('SELECT id, fieldNum, timer, inUse, player, frozen, wasEntered, runTimer FROM cokefields', {}, function(fields)
        if fields and fields[1] ~= nil then
            --print("In fields if statement")
            for k,v in pairs(fields) do
                --print("in pairs of fields for loop")
                if not openWorldFields[v.id] then
                    openWorldFields[v.id] = {}
                end

                --print("inserting into openWorldFields")
                if v.inUse == true then
                    openWorldFields[v.id] = {
                        id = v.id,
                        inUse = v.inUse,
                        timer = v.timer,
                        player = v.player,
                        frozen = false,
                        wasEntered = v.wasEntered,
                        runTimer = true,
                        save = true
                    }
                else
                    openWorldFields[v.id] = {
                        id = v.id,
                        inUse = v.inUse,
                        timer = v.timer,
                        player = v.player,
                        frozen = false,
                        wasEntered = v.wasEntered,
                        runTimer = false,
                        save = true
                    }
                end
                --print("openWorldFields[v.id] id", openWorldFields[v.id].id)

                if not openWorldFieldsForClients[v.id] then
                    openWorldFieldsForClients[v.id] = {}
                end
                --print("setting openWorldFieldsforclients")
                if v.inUse == true then
                    openWorldFieldsForClients[v.id] = {
                        id = v.id,
                        inUse = v.inUse,
                        timer = v.timer,
                        player = v.player,
                        frozen = false,
                        wasEntered = v.wasEntered,
                        runTimer = true,
                    }
                else
                    openWorldFieldsForClients[v.id] = {
                        id = v.id,
                        inUse = v.inUse,
                        timer = v.timer,
                        player = v.player,
                        frozen = false,
                        wasEntered = v.wasEntered,
                        runTimer = false,
                        save = true
                    }
                end
            end

        end
        GlobalState.openWorldFieldsReady = true
    end)
    FieldUpdateTick()
end)

function FieldUpdateTick()
    Citizen.CreateThread(function()
        Citizen.Wait(60*1000)
    end)
    Citizen.CreateThread(function()
        while true do
            --print("in field update tick logic")
            for k,v in pairs(openWorldFields) do
                if openWorldFields[k].runTimer == true then
                    --print("run timer = true in update tick")
                    if (openWorldFields[k].inUse == true and openWorldFields[k].wasEntered == false) then
                        local progress = openWorldFields[k].timer
                        if progress < 45 then
                           --print("increment timer")
                            openWorldFields[k].timer = progress + 1
                            openWorldFields[k].save = true
                        elseif progress >= 45 then
                            TriggerClientEvent("drugs:cokefieldSendAbandonded", -1, k, openWorldFields[k].player)
                            openWorldFields[k].timer = 0
                            openWorldFields[k].save = true
                            openWorldFields[k].inUse = false
                            openWorldFields[k].runTimer = false
                            openWorldFields[k].player = nil
                            openWorldFields[k].wasEntered = false
                            startFieldFrozenTimer(k)
                        end
                    elseif (openWorldFields[k].inUse == true and openWorldFields[k].wasEntered == true) then
                        local progress = openWorldFields[k].timer
                        if progress < 10 then
                            --print("increment timer")
                            openWorldFields[k].timer = progress + 1
                            openWorldFields[k].save = true
                        elseif progress >= 10 then
                            TriggerClientEvent("drugs:cokefieldSendAbandonded", -1, k, openWorldFields[k].player)
                            openWorldFields[k].timer = 0
                            openWorldFields[k].save = true
                            openWorldFields[k].inUse = false
                            openWorldFields[k].runTimer = false
                            openWorldFields[k].player = nil
                            openWorldFields[k].wasEntered = false
                            startFieldFrozenTimer(k)
                        end
                    end
                end
            end
            Citizen.Wait(60*1000)
        end
    end)
end

TMC.Functions.RegisterServerEvent('drugs:cokeresetfieldtimer', function(src, field, inUse)
    local harvestrep
    local playersrc = TMC.Functions.GetPlayer(src)
    if playersrc.PlayerData.rep['cokeharvest'] > 2250 then
        harvestrep = 2250
    else
        harvestrep = playersrc.PlayerData.rep['cokeharvest']
    end

    local maxFields = 0
    for k,v in pairs(Config.Cocaine['Options']['MaxHarvestZones'][field]) do
        if v[1] >= harvestrep then
            maxFields = v[2]
            --print("harvest rep:", harvestrep)
            --print("k:", k)
            --print('maxFields value:', maxFields)
            break
        end
    end

    local player = TMC.Functions.GetPlayer(src).PlayerData.citizenid
    if (maxFields - fieldCounts[field]) >= 10 then
        openWorldFields[field].inUse = inUse
        openWorldFields[field].timer = 0
        openWorldFields[field].player = player
        openWorldFields[field].frozen = false
        openWorldFields[field].wasEntered = true
        openWorldFields[field].runTimer = true
        openWorldFields[field].save = true
    end

end)

TMC.Functions.RegisterServerEvent('drugs:startfieldtimer', function(src, field)
    --print("start field timer")
    --print("field in timer ", field)
    openWorldFields[field].runTimer = true
    openWorldFields[field].save = true
end)

TMC.Functions.RegisterServerEvent('drugs:stopfieldtimer', function(src, field)
    openWorldFields[field].runTimer = false
    openWorldFields[field].timer = 0
    openWorldFields[field].save = true
end)

--------------------------------------------------------------------------------------------------------------------------------------------------------------------------

function startFieldFrozenTimer(field)
    openWorldFields[field].frozen = true
    openWorldFields[field].save = true
    SetTimeout(1000 * 60 * 20, function()
        openWorldFields[field].frozen = false
        openWorldFields[field].save = true
        if playerWithDropOff == nil then
            findNextInQueue()
        end
    end)
end

function findNextInQueue()
    if playerQueue[1] ~= nil then
        --print('find next in queue playerqueue not nil')
        playerWithDropOff = playerQueue[1]
        TriggerClientEvent('drugs:cokesendDropOff', playerQueue[1])
        cashBinTimer(playerWithDropOff)
        table.remove(playerQueue, 1)
    else
        print('queue empty')
    end
end

function cashBinTimer(src)
    SetTimeout(1000 * 60 * 13, function()
        --print('cashbintimer set timeout')
        if finishcashdropoff == false then
            findNextInQueue()
            TriggerClientEvent('drugs:cokedropofffail', src)
        end
    end)
end

TMC.Functions.RegisterServerEvent('drugs:cokezoneregister', function(src, zone, enter)
    --print("entered cokezoneregister")
    --print("zone: ", zone)
    local playerId = TMC.Functions.GetPlayer(src).PlayerData.citizenid
    if enter then
        if playersInField[zone] == nil then
            playersInField[zone] = {}
            playersInField[zone].count = 0
            playersInField[zone][playerId] = true
            playersInField[zone].count = playersInField[zone].count + 1
        else
            playersInField[zone][playerId] = true
            playersInField[zone].count = playersInField[zone].count + 1
        end
    else
        if playersInField[zone][playerId] ~= nil then
            playersInField[zone][playerId] = nil
            playersInField[zone].count = playersInField[zone].count - 1
        end
    end
end)

TMC.Functions.RegisterServerCallback('drugs:cokeFieldAssignedCallback', function(src, cb)
    player = TMC.Functions.GetPlayer(src).PlayerData.citizenid
    for k,v in pairs(openWorldFields) do
        if v.player == player then
            cb(k)
        end
    end
end)

TMC.Functions.RegisterServerCallback('drugs:cokeFieldCountCallback', function(src, cb, zone)
    player = TMC.Functions.GetPlayer(src).PlayerData.citizenid
    --print("in field callback")
    cb(fieldCounts[zone])
end)

TMC.Functions.RegisterServerCallback('drugs:cokebadleafcallback', function(src, cb)
    --print("in bad leaf callback")
    local player = TMC.Functions.GetPlayer(src)
    local badLeafCount = 0
    local items = player.PlayerData.items

    for k,v in pairs(items) do
        for index, item in pairs(v) do
            if item.name == 'cokeleaf' and TMC.Common.GetItemQuality(item.info) == 0 then
                badLeafCount = badLeafCount + 1
            end
        end
    end
    Citizen.Wait(300)
    cb(badLeafCount)
end)

TMC.Functions.RegisterServerCallback('drugs:cokezonecallback', function(src, cb, zone, pos)
    local ownerInField = false
    local playerId = TMC.Functions.GetPlayer(src).PlayerData.citizenid
    if playersInField[zone].count >= Config.Cocaine.Options.RequiredFieldPlayers then
        --print('zone', zone)
        --print('field owner id', openWorldFields[zone].player)
        --print('playerId: ', playerId)
        --print("field id: ", openWorldFields[zone].id)
        for k,v in pairs(playersInField[zone])do
            --print("players in field k:", k)
            if k == openWorldFields[zone].player then
                ownerInField = true
                --print('ownerInField', ownerInField)
                break
            end
        end
        if playersInField[zone][playerId] then
            if ownerInField then
                cb({
                    success = true,
                    hasAccess = true,
                    agro = 0
                })
            else
                cb({
                    success = true,
                    hasAccess = false,
                    agro = 1
                })
            end
        else
            cb({
                success = true,
                hasAccess = false,
                agro = 2
            })
        end
    else
        cb({
            success = false,
            hasAccess = false,
            agro = 0
        })
    end
end)

local resetTime = 45
Citizen.CreateThread(function()
	while true do
		Citizen.Wait(60*1000)
		for k,v in pairs(cooldown) do
			if GetGameTimer() - v.time >= resetTime * 60 * 1000 then
                --print("cooldown timer started")
				cokefield[v.field][v.pos] = nil
				cooldown[k] = nil
                fieldCounts[k] = 0
				TriggerClientEvent("drugs:cokefieldUpdateFieldData", -1, cokefield) -- update
			end
		end
	end
end)

function randomRepChance(src, rep, percent)
    local randomNumber = TMC.Common.TrueRandom(1, 100)
    if randomNumber <= percent then
        TMC.Functions.TriggerEvent('drugs:addrep', src, rep)
    end
end

Citizen.CreateThread(function()
    GlobalState.CocaineStartPed = os.date("%A")

    while true do
    	Citizen.Wait(10000)
		local nextCokeUpdate = os.date("%A")
    	if GlobalState.CocaineStartPed ~= nextCokeUpdate then
			GlobalState.CocaineStartPed = nextCokeUpdate -- only update state bag if it has changed
		end
    end
end)

TMC.Functions.RegisterServerEvent("drugs:cokeResetFieldCount", function(src, field)
    fieldCounts[field] = 0
end)

TMC.Functions.RegisterServerEvent("drugs:cokeAttemptLeafClip", function(source, pos, field, amount)
    local found = false
	local dist = 0
    local harvestDist = 0
    local player = TMC.Functions.GetPlayer(source)
    local harvestrep
    if player.PlayerData.rep['cokeharvest'] > 2250 then
        harvestrep = 2250
    else
        harvestrep = player.PlayerData.rep['cokeharvest']
    end
    for k,v in pairs(Config.Cocaine['Options']['HarvestWidth']) do
        if v[1] >= harvestrep then
            harvestDist = v[2]
            break
        end
    end
	for k,v in pairs(cokefield[field]) do
		dist = #(pos - v.pos)
		if v.status == "harvested" then
			if dist <= harvestDist then
				found = k
				break
			end
		elseif v.status == "pending" or "processing" then
			if dist <= harvestDist then
				found = k
				break
			end
		end
	end
    if not openWorldFields[field].frozen then
	    if not found then
    		cokefield[field][pos] = {}
	    	cokefield[field][pos].group = {}
		    table.insert(cokefield[field][pos].group, source)
		    cokefield[field][pos].groupStatus = {}
		    cokefield[field][pos].status = "pending"
		    cokefield[field][pos].pos = pos
		    TriggerClientEvent("drugs:cokefieldUpdateFieldData", -1, cokefield)
		    TriggerClientEvent("drugs:cokefieldPendingGroupHarvest", source, pos)
	    else
    		if cokefield[field][found].status == "pending" then
			    if dist <= harvestDist then
    				if cokefield[field][found].group ~= nil and cokefield[field][found].group[1] ~= nil then
                        Citizen.Wait(200)
					    table.insert(cokefield[field][found].group, source)
					    for i = 1, #cokefield[field][found].group, 1 do
    						cokefield[field][found].status = "processing"
						    TriggerClientEvent("drugs:cokeStartGroupHarvest", cokefield[field][found].group[i], found, amount)
					    end
					    TriggerClientEvent("drugs:cokefieldUpdateFieldData", -1, cokefield)
				    end
			    else
    				TMC.Functions.SimpleNotify(source, "You can't harvest this close to a pending harvest", "error")
                    TriggerClientEvent("drugs:cokefieldShowPromptGroup", source)
			    end
		    elseif cokefield[field][found].status == "harvested" then
    			TMC.Functions.SimpleNotify(source, 'This area was recently harvested', 'error')
                TriggerClientEvent("drugs:cokefieldShowPromptGroup", source)
		    elseif cokefield[field][found].status == "processing" then
    			TMC.Functions.SimpleNotify(source, 'This area is currently being processed', 'error')
                TriggerClientEvent("drugs:cokefieldShowPromptGroup", source)
		    end
	    end
    else
        TMC.Functions.SimpleNotify(source, "This field is out of product, try again later.", 'error')
    end
end)

TMC.Functions.RegisterServerCallback('drugs:cokeleafcallback', function(src, cb, zone, amount, finishCollection, pos)
    local player = TMC.Functions.GetPlayer(src)
    local found = false
    local dist = 0
    local maxFields = 0

    local harvestrep
    if player.PlayerData.rep['cokeharvest'] > 2250 then
        harvestrep = 2250
    else
        harvestrep = player.PlayerData.rep['cokeharvest']
    end
    --print(Config.Cocaine['Options']['MaxHarvestZones'][zone])
    for k,v in pairs(Config.Cocaine['Options']['MaxHarvestZones'][zone]) do
        if v[1] >= harvestrep then
            maxFields = v[2]
            --print("harvest rep:", harvestrep)
            --print("k:", k)
            --print('maxFields value:', maxFields)
            break
        end
    end

    for k,v in pairs(cokefield[zone]) do
		dist = #(pos - v.pos)
		if v.status == "harvested" then
			if dist <= 4 then
				found = k
				break
			end
		elseif v.status == "pending" or "processing" then
			if dist <= 4 then
				found = k
				break
			end
		end
	end

    if not finishCollection and fieldCounts[zone] < maxFields then
        cb({
            success = true,
            finish = false
        })
    else
        TMC.Functions.TriggerEvent('drugs:cokegivefield', player, false)
        cb({
            success = false,
            finish = true
        })
    end
end)

TMC.Functions.RegisterServerEvent('drugs:cokecompleteHarvest', function(src, zone, pos, amount)
	local player = TMC.Functions.GetPlayer(src)
    table.insert(cokefield[zone][pos].groupStatus, true)
    if #cokefield[zone][pos].groupStatus == 2 then
        fieldCounts[zone] = fieldCounts[zone] + 1
        --print("field being counted:", zone, "field count", fieldCounts[zone])
        for k,v in pairs(cokefield[zone][pos].group) do
            local player = TMC.Functions.GetPlayer(v)
            if player ~= nil then
                player.Functions.AddItem("cokeleaf", amount)
                randomRepChance(src, 'cokeharvest', 33)
            end
        end

        local harvestrep
        if player.PlayerData.rep['cokeharvest'] > 2250 then
            harvestrep = 2250
        else
            harvestrep = player.PlayerData.rep['cokeharvest']
        end

        local maxFields = 0
        for k,v in pairs(Config.Cocaine['Options']['MaxHarvestZones'][zone]) do
            if v[1] >= harvestrep then
                maxFields = v[2]
                --print("harvest rep:", harvestrep)
                --print("k:", k)
                --print('maxFields value:', maxFields)
                break
            end
        end

        if maxFields - fieldCounts[zone] == 10 then
            TMC.Functions.TriggerEvent('drugs:cokeresetfieldtimer', zone, true)
        elseif maxFields - fieldCounts[zone] < 10 then
            --print("field ", zone)
            TMC.Functions.TriggerEvent("drugs:startfieldtimer", zone)
        end
        --print("starting cooldown")
        table.insert(cooldown, {field = zone, pos = pos, time = GetGameTimer()})
        cokefield[zone][pos].status = "harvested"
        TriggerClientEvent("drugs:cokefieldUpdateFieldData", -1, cokefield)
    end
end)

function GetClosestArea(pos, field)
	local closest,closestDist
	for k,v in pairs(cokefield[field]) do
		local dist =  #(pos - v.pos)
		if not closestDist or dist < closestDist then
			closestDist = dist
			closest = k
		end
	end
	if closest and closestDist then return closest,closestDist
	else return false,99999
	end
end

TMC.Functions.RegisterServerEvent('drugs:cokeleavePendingHarvest', function(source, field, pos)
	--local src = source
    --print("field", field)
    --print("position", pos)
	if cokefield[field][pos].status ~= "harvested" then
		cokefield[field][pos] = nil
        --print("src", src)
        --print("source", source)
		TMC.Functions.SimpleNotify(source, "You moved away from pending harvest area", "error")
		TriggerClientEvent("drugs:cokefieldUpdateFieldData", -1, cokefield)
	end
end)

TMC.Functions.RegisterServerEvent('drugs:cokeunlock', function(src, lab, status)
    TriggerClientEvent('drugs:unlocklab', -1, lab, status)
end)

TMC.Functions.RegisterServerEvent('drugs:cokebag', function(src, item)
    TriggerClientEvent('drugs:cokebrickbag', src, item.info.quality, item.slot, item.index)
end)

function tablefind(table, val)
    for k,v in pairs(table) do
        if val == v then
            return k
        end
    end
end

TMC.Functions.CreateUseableItem('cokebaggy', function(source, item)
    TriggerClientEvent('drugs:cokeconsumption', source, item.info.quality, item.slot, item.index)
end)

TMC.Functions.CreateUseableItem('cokebaggy_decent', function(source, item)
    TriggerClientEvent('drugs:cokeconsumption', source, item.info.quality, item.slot, item.index)
end)

TMC.Functions.CreateUseableItem('cokebaggy_good', function(source, item)
    TriggerClientEvent('drugs:cokeconsumption', source, item.info.quality, item.slot, item.index)
end)

TMC.Functions.CreateUseableItem('cokebaggy_better', function(source, item)
    TriggerClientEvent('drugs:cokeconsumption', source, item.info.quality, item.slot, item.index)
end)

TMC.Functions.CreateUseableItem('cokebaggy_best', function(source, item)
    TriggerClientEvent('drugs:cokeconsumption', source, item.info.quality, item.slot, item.index)
end)

TMC.Functions.CreateUseableItem('coke_brick', function(source, item)
    TriggerClientEvent('drugs:cokebrickbag', source, item.info.quality, item.slot, item.index)
end)


--Leaf Soaking code
Citizen.CreateThread(function()
    GlobalState.openWorldBarrelsReady = false
    TMC.Functions.ExecuteSqlSync('SELECT id, zone, data, pureity FROM cokebarrels WHERE `openWorld` = 1', {}, function(barrels)
        if barrels and barrels[1] ~= nil then
            --print("In barrels if statement")
            for k,v in pairs(barrels) do
                --print("in pairs of barrels for loop")
                v.data = TMC.Common.Decode(v.data)
                --print("Decoded v.data", v.data)
                if not openWorldBarrels[v.zone] then
                    openWorldBarrels[v.zone] = {}
                end

                --print("inserting into opernWorldBarrels")
                openWorldBarrels[v.zone][v.id] = v.data
                --print("openWorldBarrels[v.zone][v.id]", openWorldBarrels[v.zone][v.id])
                openWorldBarrels[v.zone][v.id].save = false
                --print("openWorldBarrels[v.zone][v.id]", openWorldBarrels[v.zone][v.id])

                if not openWorldBarrelsForClients[v.zone] then
                    openWorldBarrelsForClients[v.zone] = {}
                end
                --print("setting openworldbarrelsforclients")
                openWorldBarrelsForClients[v.zone][v.id] = {
                    coords = v.data.coords,
                    progress = v.data.progress,
                    barrel = v.data.barrel,
                    quality = v.data.quality
                }
            end
        end
        GlobalState.openWorldBarrelsReady = true
    end)

    BarrelUpdateTick()
end)

function BarrelUpdateTick()
    Citizen.CreateThread(function()
        Citizen.Wait(Config.Cocaine.BarrelStatusUpdateTick*60*1000)
    end)
    Citizen.CreateThread(function()
        while true do
            for zone,_ in pairs(openWorldBarrels) do
                for k,v in pairs(openWorldBarrels[zone]) do
                    if (openWorldBarrels[zone][k].progress ~= 100) then
                        local soakAmount = math.random(Config.Cocaine.SoakAmount.min, Config.Cocaine.SoakAmount.max)
                        local progress = openWorldBarrels[zone][k].progress
                        if progress + soakAmount < 100 then
                            openWorldBarrels[zone][k].progress = progress + soakAmount
                            openWorldBarrels[zone][k].save = true
                        elseif progress + soakAmount >= 100 then
                            openWorldBarrels[zone][k].progress = 100
                            openWorldBarrels[zone][k].save = true
                        end
                    end
                end
            end
            Citizen.Wait(Config.Cocaine.LeafSoakTick*60*1000)
        end
    end)
end

TMC.Functions.RegisterServerEvent("drugs:cokeharvestLeafPaste", function(source, zone, id, itemData)
    --print("in cokeharvestleafpaste")
    if not openWorldBarrels[zone] then
        return
    end
    if not openWorldBarrels[zone][id] then
        return
    end
    local barrelData = TMC.Common.CopyTable(openWorldBarrels[zone][id])
    openWorldBarrels[zone][id] = nil
    openWorldBarrelsForClients[zone][id] = nil
    if barrelData and barrelData.progress == 100 then
        TriggerClientEvent("drugs:cokeaddCokeLeafPaste", source, itemData)
        TMC.Functions.SimpleNotify(source, "Coke Leaf Paste Collected", "success")
        TMC.Functions.ExecuteSql("DELETE FROM `cokebarrels` WHERE `id` = @id", {
            ["@id"] = id,
        }, function(result)

        end)
        TriggerClientEvent("drugs:cokeFreeBarrel", -1, zone, id) --free up barrel (might change)
        --end
    end
end)

TMC.Functions.RegisterServerEvent("drugs:cokeloadBarrel", function(source, pos, zone, purity, best, curBarrel)
    local steam = TMC.Functions.GetIdentifier(source, 'steam')
    local player = TMC.Functions.GetPlayer(source)
    local leafData = {
        quality = purity,
        type = 'cokeleaf',
        progress = 0,
        coords = pos,
        save = false,
        bestRecipe = best,
        barrel = curBarrel
    }
    TMC.Functions.ExecuteSql("INSERT INTO `cokebarrels` (openWorld, zone, data, steam, pureity) VALUES (@openWorld, @zone, @data, @steam, @pureity)", {
        ["@openWorld"] = 1,
        ["@zone"] = zone,
        ["@data"] = json.encode(leafData),
        ["@steam"] = steam,
        ["@pureity"] = purity
    }, function(result)
        if not openWorldBarrels[zone] then
            openWorldBarrels[zone] = {}
        end
        openWorldBarrels[zone][result.insertId] = leafData

        if not openWorldBarrelsForClients[zone] then
            openWorldBarrelsForClients[zone] = {}
        end
        openWorldBarrelsForClients[zone][result.insertId] = {
            coords = {x = leafData.coords.x, y = leafData.coords.y, z = leafData.coords.z},
            quality = purity,
            barrel = curBarrel,
            progress = leafData.progress,
            bestRecipe = leafData.bestRecipe,
            index = result.insertId,
            inUse = true
        }
        TriggerClientEvent("drugs:cokeaddBarrel", -1, zone, result.insertId, openWorldBarrelsForClients[zone][result.insertId])
    end)
end)

TMC.Functions.RegisterServerEvent("drugs:cokegetZoneBarrels", function(source, zone)
    if not openWorldBarrels[zone] then return; end
    TriggerLatentClientEvent("drugs:cokereturnZoneBarrels", source, TMC.Config.Server.LatentBps, zone, openWorldBarrelsForClients[zone])
end)

TMC.Functions.RegisterServerEvent("drugs:cokegetLeafData", function(source, zone, id)
    if not openWorldBarrels[zone] or not openWorldBarrels[zone][id] then
        ClearPedTasks(GetPlayerPed(source))
        TMC.Functions.SimpleNotify(source, "Soaking leaves not found", "error")
        return
    end
    TriggerClientEvent("drugs:cokereturnLeafData", source, openWorldBarrels[zone][id])
end)

Citizen.CreateThread(function()
    while true do
        Citizen.Wait(5*60*1000) -- save every 5 mins
        SaveCokeBarrels()
    end
end)

Citizen.CreateThread(function()
    while true do
        Citizen.Wait(2*60*1000) -- save every 2 mins
        --print("should run saveCokeFields")
        SaveCokeFields()
    end
end)

AddEventHandler("TMC:OnServerRestart", function()
    Wait(30000)
    SaveCokeBarrels()
    SaveCokeFields()
end)

function SaveCokeBarrels()
    for zone,_ in pairs(openWorldBarrels) do
        for k,v in pairs(openWorldBarrels[zone]) do
            if v and v.save then
                local saveTab = TMC.Common.CopyTable(openWorldBarrels[zone][k])
                saveTab.save = nil
                TMC.Functions.ExecuteSql("UPDATE `cokebarrels` SET `data` = @data WHERE `id` = @id", {
                    ["@id"] = k,
                    ["@data"] = json.encode(saveTab)
                }, function(result)
                    openWorldBarrels[zone][k].save = false
                end)
            end
        end
    end
end

function SaveCokeFields()
    for k,v in pairs(openWorldFields) do
        --print("v: ", v)
        --print("v.save: ", v.save)
        --print ("v.timer: ", v.timer)
        --print("v.runTimer: ", v.runTimer)
        if v and v.save then
            local saveTab = TMC.Common.CopyTable(openWorldFields[k])
            saveTab.save = nil
            TMC.Functions.ExecuteSql("UPDATE `cokefields` SET `inUse` = @inUse, `timer` = @timer, `player` = @player, `frozen` = @frozen, `wasEntered` = @wasEntered, `runTimer` = @runTimer WHERE `id` = @id", {
                ["@id"] = k,
                ["@inUse"] = saveTab.inUse,
                ["@timer"] = saveTab.timer,
                ["@player"] = saveTab.player,
                ["@frozen"] = saveTab.frozen,
                ["@wasEntered"] = saveTab.wasEntered,
                ["@runTimer"] = saveTab.runTimer
            }, function(result)
                openWorldFields[k].save = false
            end)
        end
    end
end
