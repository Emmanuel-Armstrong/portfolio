local lockedAtms = {}
local lockedTrafficLights = {}

TMC.Functions.RegisterServerEvent('crime:hacking:atmHackReward', function (src, amount)
	if not src then return end
	local player = TMC.Functions.GetPlayer(src)
	if not player then return end

	local item = 'cash_roll'
	player.Functions.AddMoney('cash', amount, "Received Cash")
	TriggerClientEvent('TMC:SimpleNotify', src, string.format('Received $%s from the ATM', amount), 'success')
	if TMC.Common.RandomChance(1, 100, 20) then
		local rollAmount = math.random(1, 3)
		player.Functions.AddItem('cash_roll', rollAmount)
		TriggerClientEvent('TMC:SimpleNotify', src, 'You managed to also snatch some rolls of cash', 'success', 6000)
		TMC.Functions.TriggerEvent('tmc:log', 'atmhack', 'Reward', 'green', string.format('**%s**\nUser has received some rolls of cash from an ATM', TMC.Player.Log(src)))
	end
	if TMC.Common.RandomChance(1, 100, 10) then
		player.Functions.AddItem('cashband', 1)
		TriggerClientEvent('TMC:SimpleNotify', src, 'You managed to also snatch a band of notes', 'success', 6000)
		TMC.Functions.TriggerEvent('tmc:log', 'atmhack', 'Reward', 'green', string.format('**%s**\nUser has received a Band of Notes from an ATM', TMC.Player.Log(src)))
	end

	if TMC.Common.RandomChance(1, 100, 75) then
		local curRep = player.PlayerData.rep["hacker"]
		local newRep = 1
		if player.PlayerData.rep.hacker then
			newRep = newRep + player.PlayerData.rep.hacker
		end
		player.Functions.SetReputation("hacker", newRep)
	end
	TMC.Functions.TriggerEvent('tmc:log', 'atmhack', 'Reward', 'green', string.format('**%s**\nUser has received **%s** dollars from hacking into an ATM', TMC.Player.Log(src), amount))
end)

TMC.Functions.RegisterServerEvent('crime:hacking:trafficlightreward', function (src, light)
	if not src then return end
	local player = TMC.Functions.GetPlayer(src)
	if not player then return end

	if TMC.Common.RandomChance(1, 100, 25) then
		local curRep = player.PlayerData.rep["hacker"]
		local newRep = 1
		if player.PlayerData.rep.hacker then
			newRep = newRep + player.PlayerData.rep.hacker
		end
		player.Functions.SetReputation("hacker", newRep)
	end
end)

TMC.Functions.RegisterServerEvent('crime:hacking:server:changeTrafficLights', function(src, light, coords)
	TriggerClientEvent('crime:hacking:changeTrafficLights', -1, light, coords)
end)

TMC.Functions.RegisterServerCallback('crime:hacking:isAtmHacked', function(src, cb, location)
	if lockedAtms[location] then
		if lockedAtms[location] > GetGameTimer() then
			cb(true) -- locked
		else
			lockedAtms[location] = nil
			cb(false) -- unlocked
		end
	else
		cb(false) -- unlocked
	end
end)

TMC.Functions.RegisterServerEvent('crime:hacking:lockHackingAtm', function(src, location)
	lockedAtms[location] = GetGameTimer() + 4 * 60 * 60 * 1000 -- 4 Hours
end)

TMC.Functions.RegisterServerCallback('crime:hacking:isTrafficLightLocked', function(src, cb, location)
	if lockedTrafficLights[location] then
		if lockedTrafficLights[location] > GetGameTimer() then
			cb(true) -- locked
		else
			lockedTrafficLights[location] = nil
			cb(false) -- unlocked
		end
	else
		cb(false) -- unlocked
	end
end)

TMC.Functions.RegisterServerEvent('crime:hacking:lockTrafficLight', function(src, location)
	lockedTrafficLights[location] = GetGameTimer() + 2 * 60 * 60 * 1000 -- 2 Hours
end)
